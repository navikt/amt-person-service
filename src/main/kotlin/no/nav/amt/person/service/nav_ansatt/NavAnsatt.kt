package no.nav.amt.person.service.nav_ansatt

import java.util.*

data class NavAnsatt(
	val id: UUID,
	val navIdent: String,
	val navn: String,
	val epost: String?,
	val telefon: String?,
)
