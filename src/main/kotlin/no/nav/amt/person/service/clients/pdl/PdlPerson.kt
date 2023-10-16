package no.nav.amt.person.service.clients.pdl

import no.nav.amt.person.service.nav_bruker.Adressebeskyttelse
import no.nav.amt.person.service.person.model.Adresse
import no.nav.amt.person.service.person.model.Personident
import no.nav.amt.person.service.person.model.AdressebeskyttelseGradering
import no.nav.amt.person.service.person.model.erBeskyttet

data class PdlPerson(
	val fornavn: String,
	val mellomnavn: String?,
	val etternavn: String,
	val telefonnummer: String?,
	val adressebeskyttelseGradering: AdressebeskyttelseGradering?,
	val identer: List<Personident>,
	val adresse: Adresse?
) {
	fun getAdressebeskyttelse(): Adressebeskyttelse? {
		return if (adressebeskyttelseGradering?.erBeskyttet() == true) {
			Adressebeskyttelse.valueOf(adressebeskyttelseGradering.name)
		} else {
			null
		}
	}
}
