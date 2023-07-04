package no.nav.amt.person.service.clients.pdl

import no.nav.amt.person.service.person.model.Personident
import no.nav.amt.person.service.person.model.AdressebeskyttelseGradering

data class PdlPerson(
	val fornavn: String,
	val mellomnavn: String?,
	val etternavn: String,
	val telefonnummer: String?,
	val adressebeskyttelseGradering: AdressebeskyttelseGradering?,
	val identer: List<Personident>
)
