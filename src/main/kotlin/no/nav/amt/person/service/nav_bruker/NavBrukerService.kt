package no.nav.amt.person.service.nav_bruker

import no.nav.amt.person.service.clients.krr.KrrProxyClient
import no.nav.amt.person.service.config.SecureLog.secureLog
import no.nav.amt.person.service.nav_ansatt.NavAnsatt
import no.nav.amt.person.service.nav_ansatt.NavAnsattService
import no.nav.amt.person.service.nav_enhet.NavEnhet
import no.nav.amt.person.service.nav_enhet.NavEnhetService
import no.nav.amt.person.service.person.PersonService
import no.nav.amt.person.service.person.model.Person
import no.nav.poao_tilgang.client.PoaoTilgangClient
import org.springframework.stereotype.Service
import java.util.*

@Service
class NavBrukerService(
	private val repository: NavBrukerRepository,
	private val personService: PersonService,
	private val navAnsattService: NavAnsattService,
	private val navEnhetService: NavEnhetService,
	private val krrProxyClient: KrrProxyClient,
	private val poaoTilgangClient: PoaoTilgangClient,
) {

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
		val person = personService.hentEllerOpprettPerson(personIdent)
		val veileder = navAnsattService.hentBrukersVeileder(personIdent)
		val navEnhet = navEnhetService.hentNavEnhetForBruker(personIdent)
		val kontaktinformasjon = krrProxyClient.hentKontaktinformasjon(personIdent)
		val erSkjermet = poaoTilgangClient.erSkjermetPerson(personIdent).getOrThrow()

		val navBruker = NavBruker(
			id = UUID.randomUUID(),
			person = person,
			navVeileder = veileder,
			navEnhet = navEnhet,
			telefon = kontaktinformasjon.telefonnummer,
			epost = kontaktinformasjon.epost,
			erSkjermet = erSkjermet,
		)

		repository.upsert(navBruker.toUpsert())

		return navBruker
	}

	fun oppdaterNavEnhet(navBruker: NavBruker, navEnhet: NavEnhet?) {
		repository.upsert(navBruker.toUpsert(navEnhetId = navEnhet?.id))
	}

	fun finnBrukerId(gjeldendeIdent: String): UUID? {
		return repository.finnBrukerId(gjeldendeIdent)
	}

	fun oppdaterNavVeileder(navBrukerId: UUID, veileder: NavAnsatt) {
		repository.oppdaterNavVeileder(navBrukerId, veileder.id)
	}

	fun settSkjermet(brukerId: UUID, erSkjermet: Boolean) {
		repository.settSkjermet(brukerId, erSkjermet)
	}

	fun oppdaterKontaktinformasjon(personer: List<Person>) {
		personer.forEach {person ->
			repository.finnBrukerId(person.personIdent)?.let { brukerId ->
				val kontaktinformasjon = krrProxyClient.hentKontaktinformasjon(person.personIdent)
				repository.oppdaterKontaktinformasjon(
					brukerId,
					kontaktinformasjon.telefonnummer,
					kontaktinformasjon.epost,
				)

			}
		}

	}

	fun slettBrukere(personer: List<Person>) {
		personer.forEach {
			repository.deleteByPersonId(it.id)
			secureLog.info("Har slettet navbruker med personident: ${it.personIdent}")
		}

	}

}
