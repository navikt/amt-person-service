package no.nav.amt.person.service.kafka.consumer.dto

import java.time.ZonedDateTime

internal data class SisteTildeltVeilederDto(
	val aktorId: String,
	val veilederId: String,
	val tilordnet: ZonedDateTime,
)
