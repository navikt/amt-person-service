package no.nav.amt.person.service.nav_enhet

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.amt.person.service.clients.norg.NorgClient
import no.nav.amt.person.service.clients.norg.NorgNavEnhet
import no.nav.amt.person.service.clients.veilarbarena.VeilarbarenaClient
import no.nav.amt.person.service.data.TestData
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class NavEnhetServiceTest {

	lateinit var norgClient: NorgClient
	lateinit var navEnhetRepository: NavEnhetRepository
	lateinit var veilarbarenaClient: VeilarbarenaClient
	lateinit var service: NavEnhetService

	@BeforeEach
	fun setup() {
		norgClient = mockk()
		navEnhetRepository = mockk(relaxUnitFun = true)
		veilarbarenaClient = mockk()
		service = NavEnhetService(
			navEnhetRepository = navEnhetRepository,
			norgClient = norgClient,
			veilarbarenaClient = veilarbarenaClient,
		)
	}

	@Test
	fun `hentNavEnhetForBruker - enhet finnes ikke - skal opprette enhet`() {
		val enhet = TestData.lagNavEnhet()
		val personIdent = "FNR"

		every { veilarbarenaClient.hentBrukerOppfolgingsenhetId(personIdent) } returns enhet.enhetId
		every { navEnhetRepository.get(enhet.enhetId) } returns null
		every { norgClient.hentNavEnhet(enhet.enhetId) } returns NorgNavEnhet(enhet.enhetId, enhet.navn)

		val faktiskEnhet = service.hentNavEnhetForBruker(personIdent)!!

		faktiskEnhet.enhetId shouldBe enhet.enhetId
		faktiskEnhet.navn shouldBe enhet.navn
	}


}
