package no.nav.amt.person.service.person.model

import java.util.UUID

data class Person(
	val id: UUID,
	val personIdent: String,
	val personIdentType: IdentType?,
	val fornavn: String,
	val mellomnavn: String?,
	val etternavn: String,
	val historiskeIdenter: List<String>
)
