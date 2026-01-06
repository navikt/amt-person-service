package no.nav.amt.person.service.kafka.consumer

import no.nav.amt.person.service.navbruker.NavBrukerService
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue

@Component
class SkjermetPersonConsumer(
	private val navBrukerService: NavBrukerService,
	private val objectMapper: ObjectMapper,
) {
	fun ingest(
		key: String,
		value: String,
	) {
		val erSkjermet = objectMapper.readValue<Boolean>(value)

		val brukerId = navBrukerService.finnBrukerId(key) ?: return

		navBrukerService.settSkjermet(brukerId, erSkjermet)
	}
}
