package no.nav.amt.person.service.kafka.ingestor

import no.nav.amt.person.service.kafka.ingestor.dto.DeltakerDto
import no.nav.amt.person.service.nav_bruker.NavBrukerService
import no.nav.amt.person.service.utils.JsonUtils.fromJsonString
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class EndretDeltakerIngestor(
	private val navBrukerService: NavBrukerService
) {
	private val log = LoggerFactory.getLogger(javaClass)

	fun ingest(value: String) {
		val deltakerRecord = fromJsonString<DeltakerDto>(value)
		val bruker = navBrukerService.hentNavBruker(deltakerRecord.personalia.personident)
		if (bruker == null) {
			log.warn("Fant ikke bruker for deltaker med id ${deltakerRecord.id}")
			return
		}
		if (bruker.sisteKrrSync == null || bruker.sisteKrrSync.isBefore(LocalDateTime.now().minusDays(14))) {
			navBrukerService.oppdaterKontaktinformasjon(bruker)
		}
	}
}
