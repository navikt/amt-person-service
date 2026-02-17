package no.nav.amt.person.service.integration.mock.responses

import no.nav.amt.person.service.navenhet.NavEnhetDbo
import java.util.UUID

data class MockNavEnhetResponse(
	val id: UUID,
	val enhetId: String,
	val navn: String,
) {
	companion object {
		fun fromDbo(source: NavEnhetDbo) =
			MockNavEnhetResponse(
				id = source.id,
				enhetId = source.enhetId,
				navn = source.navn,
			)
	}
}
