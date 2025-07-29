package no.nav.amt.person.service.kafka.consumer

import io.mockk.clearAllMocks
import io.mockk.mockk
import io.mockk.verify
import no.nav.amt.person.service.data.kafka.KafkaMessageCreator
import no.nav.amt.person.service.navbruker.NavBrukerService
import no.nav.amt.person.service.navenhet.NavEnhetService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class EndringPaaBrukerConsumerTest {
	private val navBrukerService: NavBrukerService = mockk()
	private val navEnhetService: NavEnhetService = mockk()
	private val endringPaaBrukerConsumer = EndringPaaBrukerConsumer(navBrukerService, navEnhetService)

	@BeforeEach
	fun setup() = clearAllMocks()

	@Test
	fun `ingest - nav enhet mangler i melding - endrer ikke nav enhet`() {
		endringPaaBrukerConsumer.ingest(
			KafkaMessageCreator.lagEndringPaaBrukerMsg(oppfolgingsenhet = null).toJson(),
		)

		verify(exactly = 0) { navBrukerService.hentNavBruker(any<String>()) }
		verify(exactly = 0) { navEnhetService.hentEllerOpprettNavEnhet(any()) }
		verify(exactly = 0) { navBrukerService.oppdaterNavEnhet(any(), any()) }
	}
}
