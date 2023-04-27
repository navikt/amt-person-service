package no.nav.amt.person.service.kafka.ingestor

import io.mockk.mockk
import io.mockk.verify
import no.nav.amt.person.service.data.kafka.KafkaMessageCreator
import no.nav.amt.person.service.nav_bruker.NavBrukerService
import no.nav.amt.person.service.nav_enhet.NavEnhetService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class EndringPaaBrukerIngestorTest {
	lateinit var navBrukerService: NavBrukerService
	lateinit var navEnhetService: NavEnhetService
	lateinit var endringPaaBrukerIngestor: EndringPaaBrukerIngestor

	@BeforeEach
	fun setup() {
		navBrukerService = mockk()
		navEnhetService = mockk()
		endringPaaBrukerIngestor = EndringPaaBrukerIngestor(navBrukerService, navEnhetService)
	}

	@Test
	fun `ingest - nav enhet mangler i melding - endrer ikke nav enhet`() {
		endringPaaBrukerIngestor.ingest(
			KafkaMessageCreator.lagEndringPaaBrukerMsg(oppfolgingsenhet = null).toJson()
		)

		verify(exactly = 0) { navBrukerService.hentNavBruker(any<String>()) }
		verify(exactly = 0) { navEnhetService.hentEllerOpprettNavEnhet(any()) }
		verify(exactly = 0) { navBrukerService.oppdaterNavEnhet(any(), any()) }
	}
}
