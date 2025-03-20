package no.nav.amt.person.service.kafka.consumer.dto

data class EndringPaaBrukerDto(
	val fodselsnummer: String,
	val oppfolgingsenhet: String?,
)
