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

	fun hentNavBruker(personIdent: String): NavBruker? {
		return repository.get(personIdent)?.toModel()
	}

	fun hentEllerOpprettNavBruker(personIdent: String): NavBruker {
		return repository.get(personIdent)?.toModel() ?: opprettNavBruker(personIdent)
	}

	private fun opprettNavBruker(personIdent: String): NavBruker {
		val personOpplysninger = pdlClient.hentPerson(personIdent)

		if (personOpplysninger.adressebeskyttelseGradering.erBeskyttet()) {
			throw IllegalStateException("Nav bruker er adreessebeskyttet og kan ikke lagres")
		}

		val person = personService.hentEllerOpprettPerson(personIdent, personOpplysninger)
		val veileder = navAnsattService.hentBrukersVeileder(personIdent)
		val navEnhet = navEnhetService.hentNavEnhetForBruker(personIdent)
		val kontaktinformasjon = krrProxyClient.hentKontaktinformasjon(personIdent).getOrNull()
		val erSkjermet = poaoTilgangClient.erSkjermetPerson(personIdent).getOrThrow()

		val navBruker = NavBruker(
			id = UUID.randomUUID(),
			person = person,
			navVeileder = veileder,
			navEnhet = navEnhet,
			telefon = kontaktinformasjon?.telefonnummer ?:  personOpplysninger.telefonnummer,
			epost = kontaktinformasjon?.epost,
			erSkjermet = erSkjermet,
		)

		upsert(navBruker)
		rolleService.opprettRolle(person.id, Rolle.NAV_BRUKER)
		log.info("Opprettet ny nav bruker med id: ${navBruker.id}")

		return navBruker
	}

	fun upsert(navBruker: NavBruker) {
		transactionTemplate.executeWithoutResult {
			repository.upsert(navBruker.toUpsert())
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
			oppdaterKontaktinformasjon(person.personIdent)
		}
	}

	private fun oppdaterKontaktinformasjon(personIdent: String) {
		val bruker = repository.get(personIdent)?.toModel() ?: return

		val krrKontaktinfo = krrProxyClient.hentKontaktinformasjon(personIdent).getOrElse {
			val feilmelding = "Klarte ikke hente kontaktinformasjon fra KRR-Proxy: ${it.message}"

			if (EnvUtils.isDev()) log.info(feilmelding)
			else log.error(feilmelding)

			return
		}

		val telefon = krrKontaktinfo.telefonnummer ?: pdlClient.hentTelefon(personIdent)

		if (bruker.telefon == telefon && bruker.epost == krrKontaktinfo.epost) return

		upsert(bruker.copy(telefon = telefon, epost = krrKontaktinfo.epost))
	}

	fun slettBrukere(personer: List<Person>) {
		personer.forEach {
			val bruker = repository.get(it.personIdent)?.toModel()

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

		secureLog.info("Slettet navbruker med personident: ${bruker.person.personIdent}")
		log.info("Slettet navbruker med personId: ${bruker.person.id}")
	}

	@TransactionalEventListener
	fun onPersonUpdate(personUpdateEvent: PersonUpdateEvent) {
		repository.get(personUpdateEvent.person.personIdent)?.let {
			kafkaProducerService.publiserNavBruker(it.toModel())
		}
	}

}
