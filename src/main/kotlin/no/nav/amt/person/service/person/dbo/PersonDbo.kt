package no.nav.amt.person.service.person.dbo

import java.time.LocalDateTime
import java.util.UUID

data class PersonDbo(
	val id: UUID,
	val personident: String,
	val fornavn: String,
	val mellomnavn: String?,
	val etternavn: String,
	val createdAt: LocalDateTime = LocalDateTime.now(),
	val modifiedAt: LocalDateTime = LocalDateTime.now(),
) {
	fun erUkjent() = etternavn.equals(UNKNOWN_NAME, ignoreCase = true)

	companion object {
		const val UNKNOWN_NAME = "Ukjent"
	}
}
