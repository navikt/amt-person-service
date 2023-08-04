package no.nav.amt.person.service.controller.dto

import no.nav.amt.person.service.person.model.Person
import java.util.UUID

data class ArrangorAnsattDto (
	val id: UUID,
	val personident: String,
	val fornavn: String,
	val mellomnavn: String?,
	val etternavn: String,
)

fun Person.toArrangorAnsattDto() = ArrangorAnsattDto(
	id = id,
	personident = personident,
	fornavn = fornavn,
	mellomnavn = mellomnavn,
	etternavn = etternavn,
)
