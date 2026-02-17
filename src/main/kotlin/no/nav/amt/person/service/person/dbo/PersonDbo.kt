package no.nav.amt.person.service.person.dbo

import no.nav.amt.person.service.clients.pdl.PdlPerson.Companion.UNKNOWN_NAME
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
	fun erUkjent() = etternavn == UNKNOWN_NAME
}
