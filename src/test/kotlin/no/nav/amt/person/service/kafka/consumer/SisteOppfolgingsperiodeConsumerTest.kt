package no.nav.amt.person.service.kafka.consumer

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.amt.person.service.data.kafka.KafkaMessageCreator
import no.nav.amt.person.service.navbruker.NavBrukerRepository
import no.nav.amt.person.service.navbruker.NavBrukerService
import no.nav.amt.person.service.navenhet.NavEnhetService
import no.nav.amt.person.service.utils.JsonUtils.staticObjectMapper
import no.nav.security.mock.oauth2.http.objectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SisteOppfolgingsperiodeConsumerTest {
    private val navBrukerRepository: NavBrukerRepository = mockk(relaxUnitFun = true)
    private val navBrukerService: NavBrukerService = mockk()
    private val navEnhetService: NavEnhetService = mockk()
    private val sisteOppfolgingsperiodeConsumer =
        SisteOppfolgingsperiodeConsumer(
            navBrukerRepository = navBrukerRepository,
            navBrukerService = navBrukerService,
            navEnhetService = navEnhetService,
            objectMapper = staticObjectMapper,
        )

    @BeforeEach
    fun setup() = clearAllMocks()

    @Test
    fun `ingest - bruker finnes ikke - endrer ikke nav enhet`() {
        val msg = KafkaMessageCreator.lagSisteOppfolgingsperiodeMsg()

        every { navBrukerRepository.get(msg.ident) } returns null

        sisteOppfolgingsperiodeConsumer.ingest(objectMapper.writeValueAsString(msg))

        verify(exactly = 0) {
            navEnhetService.hentEllerOpprettNavEnhet(any())
            navBrukerService.upsert(any())
        }
    }
}
