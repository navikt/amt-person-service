package no.nav.amt.person.service.navenhet

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.amt.person.service.clients.norg.NorgClient
import no.nav.amt.person.service.clients.norg.NorgNavEnhet
import no.nav.amt.person.service.clients.veilarbarena.VeilarbarenaClient
import no.nav.amt.person.service.data.TestData
import no.nav.amt.person.service.kafka.producer.KafkaProducerService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class NavEnhetServiceTest {
	private val norgClient: NorgClient = mockk()
	private val navEnhetRepository: NavEnhetRepository = mockk(relaxUnitFun = true)
	private val veilarbarenaClient: VeilarbarenaClient = mockk()
	private val kafkaProducerService = mockk<KafkaProducerService>(relaxUnitFun = true)

	private val service =
		NavEnhetService(
			navEnhetRepository = navEnhetRepository,
			norgClient = norgClient,
			veilarbarenaClient = veilarbarenaClient,
			kafkaProducerService,
		)

	@BeforeEach
	fun setup() = clearAllMocks()

	@Test
	fun `hentNavEnhetForBruker - enhet finnes ikke - skal opprette enhet`() {
		val enhet = TestData.lagNavEnhet()
		val personident = "FNR"

		every { veilarbarenaClient.hentBrukerOppfolgingsenhetId(personident) } returns enhet.enhetId
		every { navEnhetRepository.get(enhet.enhetId) } returns null
		every { norgClient.hentNavEnhet(enhet.enhetId) } returns NorgNavEnhet(enhet.enhetId, enhet.navn)

		val faktiskEnhet = service.hentNavEnhetForBruker(personident)!!
		assertSoftly(faktiskEnhet.shouldNotBeNull()) {
			enhetId shouldBe enhet.enhetId
			navn shouldBe enhet.navn
		}
		verify { kafkaProducerService.publiserNavEnhet(faktiskEnhet) }
	}

	@Test
	fun `oppdaterNavEnheter - enhet med nytt navn - oppdaterer enhet`() {
		val enhet1 = TestData.lagNavEnhet(navn = "NAV Test 1").toModel()
		val enhet2 = TestData.lagNavEnhet(navn = "NAV Test 2").toModel()

		val oppdatertEnhet1 = NorgNavEnhet(enhet1.enhetId, "Nytt Navn")

		every { norgClient.hentNavEnheter(listOf(enhet1.enhetId, enhet2.enhetId)) } returns
			listOf(
				oppdatertEnhet1,
				NorgNavEnhet(enhet2.enhetId, enhet2.navn),
			)

		service.oppdaterNavEnheter(listOf(enhet1, enhet2))

		val enhet1MedNyttNavn = enhet1.copy(navn = "Nytt Navn")
		verify(exactly = 1) { navEnhetRepository.update(enhet1MedNyttNavn) }
		verify(exactly = 1) { kafkaProducerService.publiserNavEnhet(enhet1MedNyttNavn) }
		verify(exactly = 0) { navEnhetRepository.update(enhet2) }
	}
}
