package no.nav.amt.person.service.navenhet

import java.util.UUID

data class NavEnhet(
	val id: UUID,
	val enhetId: String,
	val navn: String,
)
