package no.nav.amt.person.service.data.kafka.message

data class EndringPaaBrukerMsg(
	val fodselsnummer: String,
	val oppfolgingsenhet: String?,
)
