package no.nav.amt.person.service.kafka.ingestor

import no.nav.amt.person.service.kafka.ingestor.dto.DeltakerDto
import no.nav.amt.person.service.nav_bruker.NavBrukerService
import no.nav.amt.person.service.utils.JsonUtils.fromJsonString
import org.springframework.stereotype.Service

@Service
class EndretDeltakerIngestor(
	private val navBrukerService: NavBrukerService
) {
	fun ingest(value: String) {
		val deltakerRecord = fromJsonString<DeltakerDto>(value)
		navBrukerService.oppdaterKontaktinformasjon(
			personident = deltakerRecord.personalia.personident,
			deltakerId = deltakerRecord.id
		)
	}
}
