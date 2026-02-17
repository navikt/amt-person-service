package no.nav.amt.person.service.testmodels

import no.nav.amt.person.service.navansatt.NavAnsattDbo
import java.util.UUID

data class NavAnsatt(
	val id: UUID,
	val navIdent: String,
	val navn: String,
	val epost: String?,
	val telefon: String?,
	val navEnhetId: UUID?,
) {
	companion object {
		fun fromDbo(source: NavAnsattDbo) =
			NavAnsatt(
				id = source.id,
				navIdent = source.navIdent,
				navn = source.navn,
				epost = source.epost,
				telefon = source.telefon,
				navEnhetId = source.navEnhetId,
			)
	}
}
