package no.nav.amt.person.service.nav_bruker

import no.nav.amt.person.service.clients.krr.KrrProxyClient
import no.nav.amt.person.service.clients.pdl.PdlClient
import no.nav.amt.person.service.config.SecureLog.secureLog
import no.nav.amt.person.service.kafka.producer.KafkaProducerService
import no.nav.amt.person.service.nav_ansatt.NavAnsatt
import no.nav.amt.person.service.nav_ansatt.NavAnsattService
import no.nav.amt.person.service.nav_enhet.NavEnhet
import no.nav.amt.person.service.nav_enhet.NavEnhetService
import no.nav.amt.person.service.person.PersonService
import no.nav.amt.person.service.person.PersonUpdateEvent
import no.nav.amt.person.service.person.RolleService
import no.nav.amt.person.service.person.model.Person
import no.nav.amt.person.service.person.model.Rolle
import no.nav.amt.person.service.person.model.erBeskyttet
import no.nav.amt.person.service.utils.EnvUtils
import no.nav.poao_tilgang.client.PoaoTilgangClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.event.TransactionalEventListener
import org.springframework.transaction.support.TransactionTemplate
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
	private val kafkaProducerService: KafkaProducerService,
	private val transactionTemplate: TransactionTemplate,
) {

	private val log = LoggerFactory.getLogger(javaClass)

	fun hentNavBruker(id: UUID): NavBruker {
		return repository.get(id).toModel()
	}

	fun hentNavBruker(personident: String): NavBruker? {
		return repository.get(personident)?.toModel()
	}

	fun hentEllerOpprettNavBruker(personident: String): NavBruker {
		return repository.get(personident)?.toModel() ?: opprettNavBruker(personident)
	}

	private fun opprettNavBruker(personident: String): NavBruker {
		val personOpplysninger = pdlClient.hentPerson(personident)

		if (personOpplysninger.adressebeskyttelseGradering.erBeskyttet()) {
			throw IllegalStateException("Nav bruker er adreessebeskyttet og kan ikke lagres")
		}

		val person = personService.hentEllerOpprettPerson(personident, personOpplysninger)
		val veileder = navAnsattService.hentBrukersVeileder(personident)
		val navEnhet = navEnhetService.hentNavEnhetForBruker(personident)
		val kontaktinformasjon = krrProxyClient.hentKontaktinformasjon(personident).getOrNull()
		val erSkjermet = poaoTilgangClient.erSkjermetPerson(personident).getOrThrow()

		val navBruker = NavBruker(
			id = UUID.randomUUID(),
			person = person,
			navVeileder = veileder,
			navEnhet = navEnhet,
			telefon = kontaktinformasjon?.telefonnummer ?:  personOpplysninger.telefonnummer,
			epost = kontaktinformasjon?.epost,
			erSkjermet = erSkjermet,
			adresse = personOpplysninger.adresse
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

	fun settSkjermet(brukerId: UUID, erSkjermet: Boolean) {
		val bruker = repository.get(brukerId).toModel()
		if (bruker.erSkjermet != erSkjermet) {
			upsert(bruker.copy(erSkjermet = erSkjermet))
		}
	}

	fun oppdaterKontaktinformasjon(personer: List<Person>) {
		personer.forEach {person ->
			oppdaterKontaktinformasjon(person.personident)
		}
	}

	fun oppdaterAdresse(personer: List<Person>) {
		personer.forEach {person ->
			oppdaterAdresse(person.personident)
		}
	}

	private fun oppdaterKontaktinformasjon(personident: String) {
		val bruker = repository.get(personident)?.toModel() ?: return

		val krrKontaktinfo = krrProxyClient.hentKontaktinformasjon(personident).getOrElse {
			val feilmelding = "Klarte ikke hente kontaktinformasjon fra KRR-Proxy: ${it.message}"

			if (EnvUtils.isDev()) log.info(feilmelding)
			else log.error(feilmelding)

			return
		}

		val telefon = krrKontaktinfo.telefonnummer ?: pdlClient.hentTelefon(personident)

		if (bruker.telefon == telefon && bruker.epost == krrKontaktinfo.epost) return

		upsert(bruker.copy(telefon = telefon, epost = krrKontaktinfo.epost))
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

		if (bruker.adresse == personOpplysninger.adresse) return

		upsert(bruker.copy(adresse = personOpplysninger.adresse))
	}

	fun slettBrukere(personer: List<Person>) {
		personer.forEach {
			val bruker = repository.get(it.personident)?.toModel()

			if (bruker != null) {
				slettBruker(bruker)
			}
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

}
