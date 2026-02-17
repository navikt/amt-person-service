package no.nav.amt.person.service.testmodels

import no.nav.amt.person.service.navenhet.NavEnhetDbo
import java.util.UUID

data class NavEnhet(
	val id: UUID,
	val enhetId: String,
	val navn: String,
) {
	companion object {
		fun fromDbo(source: NavEnhetDbo) =
			NavEnhet(
				id = source.id,
				enhetId = source.enhetId,
				navn = source.navn,
			)
	}
}
