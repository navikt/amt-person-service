package no.nav.amt.person.service.person.dbo

import no.nav.amt.person.service.person.model.IdentType
import no.nav.amt.person.service.person.model.Person
import java.time.LocalDateTime
import java.util.*

data class PersonDbo(
	val id: UUID,
	val personIdent: String,
	val personIdentType: IdentType?,
	val fornavn: String,
	val mellomnavn: String?,
	val etternavn: String,
	val historiskeIdenter: List<String>,
	val createdAt: LocalDateTime,
	val modifiedAt: LocalDateTime,
) {
	fun toModel(): Person {
		return Person(
			id,
			personIdent,
			personIdentType,
			fornavn,
			mellomnavn,
			etternavn,
			historiskeIdenter
		)
	}
}


