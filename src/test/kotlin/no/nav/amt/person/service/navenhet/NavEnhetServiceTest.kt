package no.nav.amt.person.service.navenhet

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.amt.person.service.clients.norg.NorgClient
import no.nav.amt.person.service.clients.norg.NorgNavEnhetDto
import no.nav.amt.person.service.clients.oppfolgningskontor.Arbeidsoppfolging
import no.nav.amt.person.service.clients.oppfolgningskontor.OppfolgningskontorClient
import no.nav.amt.person.service.data.TestData
import no.nav.amt.person.service.kafka.producer.KafkaProducerService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class NavEnhetServiceTest {
    private val norgClient: NorgClient = mockk()
    private val navEnhetRepository: NavEnhetRepository = mockk(relaxUnitFun = true)
    private val oppfolgningskontorClient: OppfolgningskontorClient = mockk()
    private val kafkaProducerService = mockk<KafkaProducerService>(relaxUnitFun = true)

    private val service =
        NavEnhetService(
            navEnhetRepository = navEnhetRepository,
            norgClient = norgClient,
            oppfolgningskontorClient = oppfolgningskontorClient,
            kafkaProducerService,
        )

    @BeforeEach
    fun setup() = clearAllMocks()

    @Test
    fun `hentNavEnhetForBruker - enhet finnes ikke - skal opprette enhet`() {
        val navEnhet = TestData.lagNavEnhet()
        val personident = "FNR"

        every { oppfolgningskontorClient.hentKontorForBruker(personident) } returns Arbeidsoppfolging(navEnhet.enhetId, navEnhet.navn)
        every { navEnhetRepository.get(navEnhet.enhetId) } returns null
        every { norgClient.hentNavEnhet(navEnhet.enhetId) } returns NorgNavEnhetDto.fromDbo(navEnhet)

        val faktiskEnhet = service.hentNavEnhetForBruker(personident)
        assertSoftly(faktiskEnhet.shouldNotBeNull()) {
            enhetId shouldBe navEnhet.enhetId
            navn shouldBe navEnhet.navn
        }

        verify { kafkaProducerService.publiserNavEnhet(faktiskEnhet) }
    }

    @Test
    fun `hentNavEnhetForBruker - bruker har ingen arbeidsoppfolgingsenhet - skal returnere null`() {
        val personident = "FNR"

        every { oppfolgningskontorClient.hentKontorForBruker(personident) } returns null

        val faktiskEnhet = service.hentNavEnhetForBruker(personident)

        faktiskEnhet shouldBe null
    }

    @Test
    fun `oppdaterNavEnheter - enhet med nytt navn - oppdaterer enhet`() {
        val enhet1 = TestData.lagNavEnhet(navn = "NAV Test 1")
        val enhet2 = TestData.lagNavEnhet(navn = "NAV Test 2")

        val oppdatertEnhet1 = NorgNavEnhetDto.fromDbo(enhet1.copy(navn = "Nytt Navn"))

        every { norgClient.hentNavEnheter(listOf(enhet1.enhetId, enhet2.enhetId)) } returns
            listOf(
                oppdatertEnhet1,
                NorgNavEnhetDto.fromDbo(enhet2),
            )

        service.oppdaterNavEnheter(listOf(enhet1, enhet2))

        val enhet1MedNyttNavn = enhet1.copy(navn = "Nytt Navn")
        verify(exactly = 1) {
            navEnhetRepository.update(enhet1MedNyttNavn)
            kafkaProducerService.publiserNavEnhet(enhet1MedNyttNavn)
        }
        verify(exactly = 0) { navEnhetRepository.update(enhet2) }
    }
}
