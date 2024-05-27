package no.nav.amt.person.service.kafka.ingestor

import no.nav.amt.person.service.kafka.ingestor.dto.DeltakerDto
import no.nav.amt.person.service.nav_bruker.NavBruker
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
		if (kontaktinformasjonErUtdatert(bruker)) {
			navBrukerService.oppdaterKontaktinformasjon(bruker)
		}
	}

	private fun kontaktinformasjonErUtdatert(navBruker: NavBruker): Boolean {
		return navBruker.sisteKrrSync == null || navBruker.sisteKrrSync.isBefore(LocalDateTime.now().minusDays(7)) ||
			navBruker.telefon == null || navBruker.epost == null
	}
}
