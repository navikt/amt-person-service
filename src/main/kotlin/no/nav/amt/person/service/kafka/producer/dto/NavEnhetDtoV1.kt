package no.nav.amt.person.service.kafka.producer.dto

import java.util.UUID

data class NavEnhetDtoV1(
	val id: UUID,
	val enhetId: String,
	val navn: String,
)
