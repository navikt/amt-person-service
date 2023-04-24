package no.nav.amt.person.service.nav_bruker

import no.nav.amt.person.service.clients.krr.KrrProxyClient
import no.nav.amt.person.service.nav_ansatt.NavAnsattService
import no.nav.amt.person.service.nav_bruker.dbo.NavBrukerUpsert
import no.nav.amt.person.service.nav_enhet.NavEnhetService
import no.nav.amt.person.service.person.PersonService
import no.nav.poao_tilgang.client.PoaoTilgangClient
import org.springframework.stereotype.Service
import java.util.UUID

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

		repository.upsert(
			NavBrukerUpsert(
				navBruker.id,
				navBruker.person.id,
				navBruker.navVeileder?.id,
				navBruker.navEnhet?.id,
				navBruker.telefon,
				navBruker.epost,
				navBruker.erSkjermet,
			)
		)

		return navBruker
	}

}
