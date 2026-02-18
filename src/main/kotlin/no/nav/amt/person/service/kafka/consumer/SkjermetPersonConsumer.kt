package no.nav.amt.person.service.kafka.consumer

import no.nav.amt.person.service.navbruker.NavBrukerRepository
import no.nav.amt.person.service.navbruker.NavBrukerService
import no.nav.amt.person.service.utils.EnvUtils.isDev
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue

@Component
class SkjermetPersonConsumer(
	private val navBrukerRepository: NavBrukerRepository,
	private val navBrukerService: NavBrukerService,
	private val objectMapper: ObjectMapper,
) {
	private val log = LoggerFactory.getLogger(javaClass)

	fun ingest(
		key: String,
		value: String?,
	) {
		if (value == null) {
			if (isDev()) {
				log.warn("Mottok uventet tombstone for Kafka-record.")
				return
			} else {
				throw IllegalArgumentException("Kan ikke ingeste tombstone for Kafka-record.")
			}
		}

		val erSkjermet = objectMapper.readValue<Boolean>(value)

		val brukerId = navBrukerRepository.finnBrukerId(key) ?: return

		navBrukerService.settSkjermet(brukerId, erSkjermet)
	}
}
