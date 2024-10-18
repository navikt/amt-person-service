package no.nav.amt.person.service.nav_bruker

import no.nav.amt.person.service.clients.krr.Kontaktinformasjon
import no.nav.amt.person.service.clients.krr.KrrProxyClient
import no.nav.amt.person.service.clients.pdl.PdlClient
import no.nav.amt.person.service.clients.pdl.PdlPerson
import no.nav.amt.person.service.clients.veilarboppfolging.VeilarboppfolgingClient
import no.nav.amt.person.service.clients.veilarbvedtaksstotte.VeilarbvedtaksstotteClient
import no.nav.amt.person.service.config.SecureLog.secureLog
import no.nav.amt.person.service.kafka.producer.KafkaProducerService
import no.nav.amt.person.service.nav_ansatt.NavAnsatt
import no.nav.amt.person.service.nav_ansatt.NavAnsattService
import no.nav.amt.person.service.nav_enhet.NavEnhet
import no.nav.amt.person.service.nav_enhet.NavEnhetService
import no.nav.amt.person.service.person.PersonService
import no.nav.amt.person.service.person.PersonUpdateEvent
import no.nav.amt.person.service.person.RolleService
import no.nav.amt.person.service.person.model.Adresse
import no.nav.amt.person.service.person.model.Rolle
import no.nav.amt.person.service.utils.EnvUtils
import no.nav.poao_tilgang.client.PoaoTilgangClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.event.TransactionalEventListener
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDateTime
import java.util.UUID

@Service
class NavBrukerService(
	private val repository: NavBrukerRepository,
	private val personService: PersonService,
	private val navAnsattService: NavAnsattService,
	private val navEnhetService: NavEnhetService,
	private val rolleService: RolleService,
	private val krrProxyClient: KrrProxyClient,
	private val poaoTilgangClient: PoaoTilgangClient,
	private val pdlClient: PdlClient,
	private val veilarboppfolgingClient: VeilarboppfolgingClient,
	private val veilarbvedtaksstotteClient: VeilarbvedtaksstotteClient,
	private val kafkaProducerService: KafkaProducerService,
	private val transactionTemplate: TransactionTemplate,
) {

	private val log = LoggerFactory.getLogger(javaClass)

	fun get(offset: Int, limit: Int, notSyncedSince: LocalDateTime? = null): List<NavBruker> {
		return repository.getAll(offset, limit, notSyncedSince).map { it.toModel() }
	}

	fun getNavBrukere(offset: Int, limit: Int): List<NavBruker> {
		return repository.getAllNavBrukere(offset, limit).map { it.toModel() }
	}

	fun getPersonidenter(offset: Int, limit: Int, notSyncedSince: LocalDateTime? = null): List<String> {
		return repository.getPersonidenter(offset, limit, notSyncedSince).distinct()
	}

	fun hentNavBruker(id: UUID): NavBruker {
		return repository.get(id).toModel()
	}

	fun hentNavBruker(personident: String): NavBruker? {
		return repository.get(personident)?.toModel()
	}

	fun hentEllerOpprettNavBruker(personident: String): NavBruker {
		val navBruker = repository.get(personident)?.toModel()?.let {
			if (it.innsatsgruppe == null) {
				oppdaterOppfolgingsperiodeOgInnsatsgruppe(it)
				repository.get(it.id).toModel()
			} else {
				it
			}
		} ?: opprettNavBruker(personident)

		return navBruker
	}

	private fun opprettNavBruker(personident: String): NavBruker {
		val personOpplysninger = pdlClient.hentPerson(personident)

		val person = personService.hentEllerOpprettPerson(personident, personOpplysninger)
		val veileder = navAnsattService.hentBrukersVeileder(personident)
		val navEnhet = navEnhetService.hentNavEnhetForBruker(personident)
		val kontaktinformasjon = krrProxyClient.hentKontaktinformasjon(personident).getOrNull()
		val erSkjermet = poaoTilgangClient.erSkjermetPerson(personident).getOrThrow()
		val oppfolgingsperioder = veilarboppfolgingClient.hentOppfolgingperioder(personident)
		val innsatsgruppe = if (harAktivOppfolgingsperiode(oppfolgingsperioder)) {
			veilarbvedtaksstotteClient.hentInnsatsgruppe(personident)
		} else {
			// innsatsgrupper fjernes ikke når bruker går ut av oppfølging
			null
		}

		val navBruker = NavBruker(
			id = UUID.randomUUID(),
			person = person,
			navVeileder = veileder,
			navEnhet = navEnhet,
			telefon = kontaktinformasjon?.telefonnummer ?:  personOpplysninger.telefonnummer,
			epost = kontaktinformasjon?.epost,
			erSkjermet = erSkjermet,
			adresse = getAdresse(personOpplysninger),
			sisteKrrSync = LocalDateTime.now(),
			adressebeskyttelse = personOpplysninger.getAdressebeskyttelse(),
			oppfolgingsperioder = oppfolgingsperioder,
			innsatsgruppe = innsatsgruppe
		)

		upsert(navBruker)
		log.info("Opprettet ny nav bruker med id: ${navBruker.id}")

		return repository.getByPersonId(person.id)?.toModel()
			?: throw IllegalStateException(
				"Fant ikke nav-bruker for person ${person.id}, skulle ha opprettet bruker ${navBruker.id}"
			)
	}

	fun upsert(navBruker: NavBruker) {
		transactionTemplate.executeWithoutResult {
			repository.upsert(navBruker.toUpsert())
			rolleService.opprettRolle(navBruker.person.id, Rolle.NAV_BRUKER)
			kafkaProducerService.publiserNavBruker(navBruker)
		}
	}

	fun oppdaterNavEnhet(navBruker: NavBruker, navEnhet: NavEnhet?) {
		upsert(navBruker.copy(navEnhet = navEnhet))
	}

	fun finnBrukerId(gjeldendeIdent: String): UUID? {
		return repository.finnBrukerId(gjeldendeIdent)
	}

	fun oppdaterNavVeileder(navBrukerId: UUID, veileder: NavAnsatt) {
		val bruker = repository.get(navBrukerId).toModel()
		if (bruker.navVeileder?.id != veileder.id) {
			upsert(bruker.copy(navVeileder = veileder))
		}
	}

	fun oppdaterOppfolgingsperiode(navBrukerId: UUID, oppfolgingsperiode: Oppfolgingsperiode) {
		val bruker = repository.get(navBrukerId).toModel()
		val oppfolgingsperioder = bruker.oppfolgingsperioder.filter { it.id != oppfolgingsperiode.id }.plus(oppfolgingsperiode)

		if (oppfolgingsperioder.toSet() != bruker.oppfolgingsperioder.toSet()) {
			upsert(bruker.copy(oppfolgingsperioder = oppfolgingsperioder))
		}
	}

	fun oppdaterInnsatsgruppe(navBrukerId: UUID, innsatsgruppe: Innsatsgruppe) {
		val bruker = repository.get(navBrukerId).toModel()

		if (innsatsgruppe != bruker.innsatsgruppe) {
			if (harAktivOppfolgingsperiode(bruker.oppfolgingsperioder)) {
				upsert(bruker.copy(innsatsgruppe = innsatsgruppe))
			} else if (bruker.innsatsgruppe != null) {
				upsert(bruker.copy(innsatsgruppe = null))
			}
		}
	}

	fun oppdaterOppfolgingsperiodeOgInnsatsgruppe(navBruker:NavBruker) {
		val oppfolgingsperioder = veilarboppfolgingClient.hentOppfolgingperioder(navBruker.person.personident)
		val innsatsgruppe = if (harAktivOppfolgingsperiode(oppfolgingsperioder)) {
			veilarbvedtaksstotteClient.hentInnsatsgruppe(navBruker.person.personident)
		} else {
			null
		}
		if (navBruker.innsatsgruppe != innsatsgruppe || navBruker.oppfolgingsperioder != oppfolgingsperioder) {
			upsert(
				navBruker.copy(
					oppfolgingsperioder = oppfolgingsperioder,
					innsatsgruppe = innsatsgruppe
				)
			)
			log.info("Oppdatert innsatsgruppe og oppfølgingsperidoe for navbruker med id ${navBruker.id}")
		}
	}

	fun oppdaterKontaktinformasjon(bruker: NavBruker) {
		val kontaktinformasjon = krrProxyClient.hentKontaktinformasjon(bruker.person.personident).getOrElse {
			val feilmelding =
				"Klarte ikke hente kontaktinformasjon fra KRR-Proxy for bruker ${bruker.id}: ${it.message}"
			if (EnvUtils.isDev()) {
				log.info(feilmelding)
			} else {
				log.error(feilmelding)
			}
			return
		}
		val telefon = kontaktinformasjon.telefonnummer ?: pdlClient.hentTelefon(bruker.person.personident)
		oppdaterKontaktinfo(bruker, kontaktinformasjon.copy(telefonnummer = telefon))
	}

	fun settSkjermet(brukerId: UUID, erSkjermet: Boolean) {
		val bruker = repository.get(brukerId).toModel()
		if (bruker.erSkjermet != erSkjermet) {
			upsert(bruker.copy(erSkjermet = erSkjermet))
		}
	}

	fun syncKontaktinfoBulk(personident: List<String>) {
		val personerChunks = personident.chunked(250) // maksgrense på 500 hos krr, vi spør på færre for å redusere timeout-problematikk

		personerChunks.forEach { personChunk ->
			val krrKontaktinfo = krrProxyClient.hentKontaktinformasjon(personChunk.toSet()).getOrElse {
				val feilmelding = "Klarte ikke hente kontaktinformasjon fra KRR-Proxy: ${it.message}"

				if (EnvUtils.isDev()) log.info(feilmelding)
				else log.error(feilmelding)
				return
			}

			krrKontaktinfo.personer.forEach kontaktinfoPersoner@ {
					val telefon = it.value.telefonnummer ?: pdlClient.hentTelefon(it.key)
					val bruker = repository.get(it.key)?.toModel() ?: return@kontaktinfoPersoner
					oppdaterKontaktinfo(bruker, it.value.copy(telefonnummer = telefon))
			}
		}
	}

	fun oppdaterAdressebeskyttelse(personident: String) {
		val bruker = repository.get(personident)?.toModel() ?: return

		val personOpplysninger = try {
			pdlClient.hentPerson(personident)
		} catch (e: Exception) {
			val feilmelding = "Klarte ikke hente person fra PDL ved oppdatert adressebeskyttelse: ${e.message}"

			if (EnvUtils.isDev()) log.info(feilmelding)
			else log.error(feilmelding)

			return
		}

		val oppdatertAdressebeskyttelse = personOpplysninger.getAdressebeskyttelse()

		if (bruker.adressebeskyttelse == oppdatertAdressebeskyttelse) return

		upsert(bruker.copy(adressebeskyttelse = oppdatertAdressebeskyttelse, adresse = getAdresse(personOpplysninger)))
	}

	fun oppdaterAdresse(personidenter: List<String>) {
		personidenter.forEach {
			oppdaterAdresse(it)
		}
	}

	private fun oppdaterAdresse(personident: String) {
		val bruker = repository.get(personident)?.toModel() ?: return

		val personOpplysninger = try {
			pdlClient.hentPerson(personident)
		} catch (e: Exception) {
			val feilmelding = "Klarte ikke hente person fra PDL ved oppdatert adresse: ${e.message}"

			if (EnvUtils.isDev()) log.info(feilmelding)
			else log.error(feilmelding)

			return
		}

		val oppdatertAdresse = getAdresse(personOpplysninger)

		if (bruker.adresse == oppdatertAdresse) return

		upsert(bruker.copy(adresse = oppdatertAdresse))
	}

	private fun getAdresse(personopplysninger: PdlPerson): Adresse? {
		val adressebeskyttelse = personopplysninger.getAdressebeskyttelse()
		return if (adressebeskyttelse == null) {
			personopplysninger.adresse
		} else {
			null
		}
	}

	fun slettBruker(bruker: NavBruker) {
		transactionTemplate.executeWithoutResult {
			repository.delete(bruker.id)
			rolleService.fjernRolle(bruker.person.id, Rolle.NAV_BRUKER)

			if (!rolleService.harRolle(bruker.person.id, Rolle.ARRANGOR_ANSATT)) {
				personService.slettPerson(bruker.person)
			}

			kafkaProducerService.publiserSlettNavBruker(bruker.person.id)
		}

		secureLog.info("Slettet navbruker med personident: ${bruker.person.personident}")
		log.info("Slettet navbruker med personId: ${bruker.person.id}")
	}

	@TransactionalEventListener
	fun onPersonUpdate(personUpdateEvent: PersonUpdateEvent) {
		repository.get(personUpdateEvent.person.personident)?.let {
			kafkaProducerService.publiserNavBruker(it.toModel())
		}
	}

	private fun oppdaterKontaktinfo(bruker: NavBruker, kontaktinformasjon: Kontaktinformasjon) {
		if (bruker.telefon == kontaktinformasjon.telefonnummer && bruker.epost == kontaktinformasjon.epost) {
			repository.upsert(bruker.copy(sisteKrrSync = LocalDateTime.now()).toUpsert())
			return
		}

		upsert(bruker.copy(telefon = kontaktinformasjon.telefonnummer, epost = kontaktinformasjon.epost, sisteKrrSync = LocalDateTime.now()))
	}

}
