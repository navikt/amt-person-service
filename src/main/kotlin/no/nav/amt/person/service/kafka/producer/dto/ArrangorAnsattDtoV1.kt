package no.nav.amt.person.service.kafka.producer.dto

import java.util.UUID

data class ArrangorAnsattDtoV1(
	val id: UUID,
	val personident: String,
	val fornavn: String,
	val mellomnavn: String?,
	val etternavn: String,
)
