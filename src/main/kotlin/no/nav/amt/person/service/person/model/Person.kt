package no.nav.amt.person.service.person.model

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
}

