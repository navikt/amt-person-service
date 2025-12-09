package no.nav.amt.person.service.kafka.consumer

import no.nav.amt.person.service.kafka.consumer.dto.EndringPaaBrukerDto
import no.nav.amt.person.service.navbruker.NavBrukerService
import no.nav.amt.person.service.navenhet.NavEnhetService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue

@Service
class EndringPaaBrukerConsumer(
	private val navBrukerService: NavBrukerService,
	private val navEnhetService: NavEnhetService,
	private val objectMapper: ObjectMapper,
) {
	private val log = LoggerFactory.getLogger(javaClass)

	fun ingest(value: String) {
		val endringPaaBrukerPayload = objectMapper.readValue<EndringPaaBrukerDto>(value)

		// Det er ikke mulig å fjerne nav kontor i arena men det kan legges meldinger på topicen som endrer andre ting
		// og derfor ikke er relevante
		if (endringPaaBrukerPayload.oppfolgingsenhet == null) return

		val navBruker = navBrukerService.hentNavBruker(endringPaaBrukerPayload.fodselsnummer) ?: return

		if (navBruker.navEnhet?.enhetId == endringPaaBrukerPayload.oppfolgingsenhet) return

		log.info("Endrer oppfølgingsenhet på NavBruker med id=${navBruker.id}")

		val navEnhet = navEnhetService.hentEllerOpprettNavEnhet(endringPaaBrukerPayload.oppfolgingsenhet)

		navBrukerService.oppdaterNavEnhet(navBruker, navEnhet?.id)
	}
}
