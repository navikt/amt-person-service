package no.nav.amt.person.service.nav_ansatt

import java.time.LocalDateTime
import java.util.*

data class NavAnsattDbo(
	val id: UUID,
	val navIdent: String,
	val navn: String,
	val telefon: String?,
	val epost: String?,
	val createdAt: LocalDateTime,
	val modifiedAt: LocalDateTime,
) {
	fun toModel() = NavAnsatt(
		id = id,
		navIdent = navIdent,
		navn = navn,
		epost = epost,
		telefon = telefon,
	)
}
