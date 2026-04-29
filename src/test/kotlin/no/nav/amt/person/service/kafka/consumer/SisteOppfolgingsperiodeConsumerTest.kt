package no.nav.amt.person.service.kafka.consumer

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.amt.person.service.data.TestData
import no.nav.amt.person.service.data.kafka.KafkaMessageCreator
import no.nav.amt.person.service.data.kafka.message.KontorPayload
import no.nav.amt.person.service.navbruker.NavBrukerRepository
import no.nav.amt.person.service.navbruker.NavBrukerService
import no.nav.amt.person.service.navenhet.NavEnhetService
import no.nav.amt.person.service.utils.JsonUtils.staticObjectMapper
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

        sisteOppfolgingsperiodeConsumer.ingest(staticObjectMapper.writeValueAsString(msg))

        verify(exactly = 0) {
            navEnhetService.hentEllerOpprettNavEnhet(any())
            navBrukerService.upsert(any())
        }
    }

    @Test
    fun `ingest - kontor er null - endrer ikke nav enhet`() {
        val navBruker = TestData.lagNavBruker()
        val msg = KafkaMessageCreator.lagSisteOppfolgingsperiodeMsg(
            ident = navBruker.person.personident,
            kontor = null,
        )

        every { navBrukerRepository.get(msg.ident) } returns navBruker

        sisteOppfolgingsperiodeConsumer.ingest(staticObjectMapper.writeValueAsString(msg))

        verify(exactly = 0) {
            navEnhetService.hentEllerOpprettNavEnhet(any())
            navBrukerService.upsert(any())
        }
    }

    @Test
    fun `ingest - kontor er endret - oppdaterer nav enhet`() {
        val gammeltNavEnhet = TestData.lagNavEnhet()
        val nyttNavEnhet = TestData.lagNavEnhet()
        val navBruker = TestData.lagNavBruker(navEnhet = gammeltNavEnhet)
        val msg = KafkaMessageCreator.lagSisteOppfolgingsperiodeMsg(
            ident = navBruker.person.personident,
            kontor = KontorPayload(kontorId = nyttNavEnhet.enhetId, kontorNavn = nyttNavEnhet.navn),
        )

        every { navBrukerRepository.get(msg.ident) } returns navBruker
        every { navEnhetService.hentEllerOpprettNavEnhet(nyttNavEnhet.enhetId) } returns nyttNavEnhet
        every { navBrukerService.upsert(any()) } returns mockk()

        sisteOppfolgingsperiodeConsumer.ingest(staticObjectMapper.writeValueAsString(msg))

        verify(exactly = 1) {
            navEnhetService.hentEllerOpprettNavEnhet(nyttNavEnhet.enhetId)
            navBrukerService.upsert(navBruker.copy(navEnhet = nyttNavEnhet))
        }
    }

    @Test
    fun `ingest - kontor er ikke endret - gjor ingenting`() {
        val navEnhet = TestData.lagNavEnhet()
        val navBruker = TestData.lagNavBruker(navEnhet = navEnhet)
        val msg = KafkaMessageCreator.lagSisteOppfolgingsperiodeMsg(
            ident = navBruker.person.personident,
            kontor = KontorPayload(kontorId = navEnhet.enhetId, kontorNavn = navEnhet.navn),
        )

        every { navBrukerRepository.get(msg.ident) } returns navBruker

        sisteOppfolgingsperiodeConsumer.ingest(staticObjectMapper.writeValueAsString(msg))

        verify(exactly = 0) {
            navEnhetService.hentEllerOpprettNavEnhet(any())
            navBrukerService.upsert(any())
        }
    }
}
