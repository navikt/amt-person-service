package no.nav.amt.person.service.api.dto

import no.nav.amt.person.service.navansatt.NavAnsatt
import java.util.UUID

data class NavAnsattDto(
	val id: UUID,
	val navIdent: String,
	val navn: String,
	val epost: String?,
	val telefon: String?,
	val navEnhetId: UUID?,
)

fun NavAnsatt.toDto() = NavAnsattDto(id, navIdent, navn, epost, telefon, navEnhetId)
