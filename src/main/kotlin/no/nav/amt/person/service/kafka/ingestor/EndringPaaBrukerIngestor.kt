package no.nav.amt.person.service.kafka.ingestor

import no.nav.amt.person.service.kafka.ingestor.dto.EndringPaaBrukerDto
import no.nav.amt.person.service.nav_bruker.NavBrukerService
import no.nav.amt.person.service.nav_enhet.NavEnhetService
import no.nav.amt.person.service.utils.JsonUtils.fromJsonString
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class EndringPaaBrukerIngestor(
	private val navBrukerService: NavBrukerService,
	private val navEnhetService: NavEnhetService,
) {
	private val log = LoggerFactory.getLogger(javaClass)

	fun ingest(value: String) {
		val brukerRecord = fromJsonString<EndringPaaBrukerDto>(value)

		//Det er ikke mulig å fjerne nav kontor i arena men det kan legges meldinger på topicen som endrer andre ting
		//og derfor ikke er relevante
		if (brukerRecord.oppfolgingsenhet == null) return

		val navBruker = navBrukerService.hentNavBruker(brukerRecord.fodselsnummer) ?: return

		if (navBruker.navEnhet?.enhetId == brukerRecord.oppfolgingsenhet) return

		log.info("Endrer oppfølgingsenhet på NavBruker med id=${navBruker.id}")

		val navEnhet = navEnhetService.hentEllerOpprettNavEnhet(brukerRecord.oppfolgingsenhet)

		navBrukerService.oppdaterNavEnhet(navBruker, navEnhet)
	}

}
