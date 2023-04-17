package no.nav.amt.person.service.clients.pdl

data class PdlPerson(
	val fornavn: String,
	val mellomnavn: String?,
	val etternavn: String,
	val telefonnummer: String?,
	val adressebeskyttelseGradering: AdressebeskyttelseGradering?,
	val identer: List<PdlPersonIdent>
)
