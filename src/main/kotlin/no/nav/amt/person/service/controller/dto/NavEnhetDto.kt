package no.nav.amt.person.service.controller.dto

import no.nav.amt.person.service.nav_enhet.NavEnhet
import java.util.*

data class NavEnhetDto(
	val id: UUID,
	val enhetId: String,
	val navn: String,
)

fun NavEnhet.toDto() = NavEnhetDto(id, enhetId, navn)
