package no.nav.amt.person.service.person.dbo

import no.nav.amt.person.service.person.model.Person
import java.time.LocalDateTime
import java.util.UUID

data class PersonDbo(
	val id: UUID,
	val personident: String,
	val fornavn: String,
	val mellomnavn: String?,
	val etternavn: String,
	val createdAt: LocalDateTime,
	val modifiedAt: LocalDateTime,
) {
	fun toModel(): Person =
		Person(
			id,
			personident,
			fornavn,
			mellomnavn,
			etternavn,
		)
}
