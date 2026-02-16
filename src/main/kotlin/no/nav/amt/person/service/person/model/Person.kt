package no.nav.amt.person.service.person.model

import no.nav.amt.person.service.person.dbo.PersonDbo
import no.nav.amt.person.service.utils.titlecase
import java.util.UUID

data class Person(
	val id: UUID,
	val personident: String,
	var fornavn: String,
	var mellomnavn: String?,
	var etternavn: String,
) {
	init {
		fornavn = fornavn.titlecase()
		mellomnavn = mellomnavn?.titlecase()
		etternavn = etternavn.titlecase()
	}

	fun erUkjent() = etternavn == UNKNOWN_NAME

	fun toDbo() =
		PersonDbo(
			id = id,
			personident = personident,
			fornavn = fornavn,
			mellomnavn = mellomnavn,
			etternavn = etternavn,
		)

	companion object {
		const val UNKNOWN_NAME = "Ukjent"
	}
}
