package no.nav.amt.person.service.controller.dto

import no.nav.amt.person.service.person.model.Person
import java.util.*

data class ArrangorAnsattDto (
	val id: UUID,
	val personIdent: String,
	val fornavn: String,
	val mellomnavn: String?,
	val etternavn: String,
)

fun Person.toArrangorAnsattDto() = ArrangorAnsattDto(
	id = id,
	personIdent = personIdent,
	fornavn = fornavn,
	mellomnavn = mellomnavn,
	etternavn = etternavn,
)
