package no.nav.amt.person.service.clients.nom

import java.time.LocalDate

class NomClientMock : NomClient {
	override fun hentNavAnsatt(navIdent: String): NomNavAnsatt? = hentNavAnsatte(listOf(navIdent)).firstOrNull()

	override fun hentNavAnsatte(navIdenter: List<String>): List<NomNavAnsatt> =
		navIdenter.map { navIdent ->
			NomNavAnsatt(
				navIdent = navIdent,
				navn = "F_$navIdent E_$navIdent",
				epost = "F_$navIdent.E_$navIdent@trygdeetaten.no",
				telefonnummer = "12345678",
				orgTilknytning =
					listOf(
						NomQueries.HentRessurser.OrgTilknytning(
							gyldigFom = LocalDate.of(2020, 1, 1),
							gyldigTom = null,
							orgEnhet = NomQueries.HentRessurser.OrgTilknytning.OrgEnhet("0315"),
							erDagligOppfolging = true,
						),
					),
			)
		}
}
