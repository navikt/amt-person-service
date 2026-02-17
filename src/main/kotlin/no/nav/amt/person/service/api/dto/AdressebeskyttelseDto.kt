package no.nav.amt.person.service.api.dto

import no.nav.amt.person.service.person.model.AdressebeskyttelseGradering

data class AdressebeskyttelseDto(
	val gradering: AdressebeskyttelseGradering?,
) {
	companion object {
		fun fromGradering(gradering: AdressebeskyttelseGradering?) = AdressebeskyttelseDto(gradering)
	}
}
