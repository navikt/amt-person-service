package no.nav.amt.person.service.kafka.producer.dto

import java.util.UUID

data class NavAnsattDtoV1(
	val id: UUID,
	val navident: String,
	val navn: String,
	val telefon: String?,
	val epost: String?,
	val navEnhetId: UUID?,
)
