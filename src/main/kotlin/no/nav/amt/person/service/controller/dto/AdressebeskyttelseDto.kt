package no.nav.amt.person.service.controller.dto

import no.nav.amt.person.service.person.model.AdressebeskyttelseGradering

data class AdressebeskyttelseDto(
	val gradering: AdressebeskyttelseGradering?
)

fun AdressebeskyttelseGradering?.toDto() = AdressebeskyttelseDto(this)
