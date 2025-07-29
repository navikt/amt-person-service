package no.nav.amt.person.service.api.dto

import no.nav.amt.person.service.navenhet.NavEnhet
import java.util.UUID

data class NavEnhetDto(
	val id: UUID,
	val enhetId: String,
	val navn: String,
)

fun NavEnhet.toDto() = NavEnhetDto(id, enhetId, navn)
