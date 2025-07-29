package no.nav.amt.person.service.navansatt

import java.time.LocalDateTime
import java.util.UUID

data class NavAnsattDbo(
	val id: UUID,
	val navIdent: String,
	val navn: String,
	val telefon: String?,
	val epost: String?,
	val createdAt: LocalDateTime,
	val modifiedAt: LocalDateTime,
	val navEnhetId: UUID?,
) {
	fun toModel() =
		NavAnsatt(
			id = id,
			navIdent = navIdent,
			navn = navn,
			epost = epost,
			telefon = telefon,
			navEnhetId = navEnhetId,
		)
}
