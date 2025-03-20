package no.nav.amt.person.service.kafka.consumer.dto

import java.util.UUID

data class DeltakerDto(
	val id: UUID,
	val personalia: DeltakerPersonaliaDto
)

data class DeltakerPersonaliaDto(
	val personident: String
)
