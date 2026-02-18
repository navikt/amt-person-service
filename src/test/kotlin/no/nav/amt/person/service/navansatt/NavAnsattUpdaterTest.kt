package no.nav.amt.person.service.navansatt

import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.amt.deltaker.bff.utils.withLogCapture
import no.nav.amt.person.service.clients.nom.NomClientImpl
import no.nav.amt.person.service.clients.nom.NomNavAnsatt
import no.nav.amt.person.service.data.TestData
import no.nav.amt.person.service.data.TestData.navGrunerlokka
import no.nav.amt.person.service.data.TestData.orgTilknytning
import no.nav.amt.person.service.navenhet.NavEnhetService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class NavAnsattUpdaterTest {
	private val navAnsattRepository: NavAnsattRepository = mockk(relaxUnitFun = true)
	private val navAnsattService: NavAnsattService = mockk(relaxUnitFun = true)
	private val nomClient: NomClientImpl = mockk()
	private val navEnhetService: NavEnhetService = mockk()
	private val updater = NavAnsattUpdater(navAnsattRepository, navAnsattService, nomClient, navEnhetService)

	@BeforeEach
	fun setup() = clearAllMocks()

	@Test
	fun `oppdaterAlle - navIdent mangler hos Nom - logger warning`() {
		val ansatt1 = TestData.lagNavAnsatt()
		val ansatt2 = TestData.lagNavAnsatt()

		every { navAnsattRepository.getAll() } returns listOf(ansatt1, ansatt2)
		every { nomClient.hentNavAnsatte(listOf(ansatt1.navIdent, ansatt2.navIdent)) } returns
			listOf(
				NomNavAnsatt(
					navIdent = ansatt1.navIdent,
					navn = ansatt1.navn,
					telefonnummer = ansatt1.telefon,
					epost = ansatt1.epost,
					orgTilknytning = orgTilknytning,
				),
			)
		every { navEnhetService.hentEllerOpprettNavEnhet(any()) } returns navGrunerlokka

		withLogCapture(NavAnsattUpdater::class.java.name) { loggingEvents ->
			updater.oppdaterAlle()

			loggingEvents.any {
				it.message == "Fant ikke Nav-ansatt med id ${ansatt1.id} i NOM"
			} shouldBe false

			loggingEvents.any {
				it.message == "Fant ikke Nav-ansatt med id ${ansatt2.id} i NOM"
			} shouldBe true
		}
	}

	@Test
	fun `oppdaterAlle - ansatt er ikke endret - oppdaterer ikke`() {
		val ansatt = TestData.lagNavAnsatt()

		every { navAnsattRepository.getAll() } returns listOf(ansatt)
		every { nomClient.hentNavAnsatte(listOf(ansatt.navIdent)) } returns
			listOf(
				NomNavAnsatt(
					navIdent = ansatt.navIdent,
					navn = ansatt.navn,
					telefonnummer = ansatt.telefon,
					epost = ansatt.epost,
					orgTilknytning = orgTilknytning,
				),
			)
		every { navEnhetService.hentEllerOpprettNavEnhet(any()) } returns navGrunerlokka

		updater.oppdaterAlle()

		verify(exactly = 0) { navAnsattService.upsertMany(setOf(ansatt)) }
	}

	@Test
	fun `oppdaterAlle - ansatt er endret - oppdaterer ansatt`() {
		val ansatt = TestData.lagNavAnsatt()
		val oppdatertAnsatt = ansatt.copy(navn = "Foo Bar")

		every { navAnsattRepository.getAll() } returns listOf(ansatt)
		every { nomClient.hentNavAnsatte(listOf(ansatt.navIdent)) } returns
			listOf(
				NomNavAnsatt(
					navIdent = oppdatertAnsatt.navIdent,
					navn = oppdatertAnsatt.navn,
					telefonnummer = oppdatertAnsatt.telefon,
					epost = oppdatertAnsatt.epost,
					orgTilknytning = orgTilknytning,
				),
			)
		every { navEnhetService.hentEllerOpprettNavEnhet(any()) } returns navGrunerlokka

		updater.oppdaterAlle()

		verify(exactly = 1) {
			navAnsattService.upsertMany(any())
		}
	}
}
