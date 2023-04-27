package no.nav.amt.person.service.kafka.ingestor.dto

data class EndringPaaBrukerDto(
	val fodselsnummer: String,
	val oppfolgingsenhet: String?,
)
