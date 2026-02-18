package no.nav.amt.person.service.kafka.consumer.dto

data class EndringPaaBrukerPayload(
	val fodselsnummer: String,
	val oppfolgingsenhet: String?,
)
