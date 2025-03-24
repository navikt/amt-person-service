package no.nav.amt.person.service.clients.veilarbvedtaksstotte

import io.kotest.matchers.shouldBe
import no.nav.amt.person.service.nav_bruker.Innsatsgruppe
import no.nav.amt.person.service.nav_bruker.InnsatsgruppeV1
import no.nav.amt.person.service.utils.JsonUtils
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class VeilarbvedtaksstotteClientTest {
	private lateinit var server: MockWebServer
	private lateinit var client: VeilarbvedtaksstotteClient

	private val fnr = "123"

	@BeforeEach
	fun setup() {
		server = MockWebServer()
		val serverUrl = server.url("/api").toString()
		client = VeilarbvedtaksstotteClient(
			apiUrl = serverUrl,
			veilarbvedtaksstotteTokenProvider = { "VEILARBVEDTAKSSTOTTE_TOKEN" }
		)
	}

	@Test
	fun `hentSisteInnsatsgruppe - bruker har innsatsgruppe - returnerer innsatsgruppe`() {
		val siste14aVedtakDTORespons = VeilarbvedtaksstotteClient.Gjeldende14aVedtakDTO(
			innsatsgruppe = Innsatsgruppe.JOBBE_DELVIS
		)
		server.enqueue(MockResponse().setBody(JsonUtils.toJsonString(siste14aVedtakDTORespons)))

		val innsatsgruppe = client.hentGjeldendeInnsatsgruppe(fnr)

		innsatsgruppe shouldBe InnsatsgruppeV1.GRADERT_VARIG_TILPASSET_INNSATS
	}

	@Test
	fun `hentGjeldendeInnsatsgruppe - bruker har ikke innsatsgruppe - returnerer null`() {
		server.enqueue(MockResponse())

		val innsatsgruppe = client.hentGjeldendeInnsatsgruppe(fnr)

		innsatsgruppe shouldBe null
	}

	@Test
	fun `hentGjeldendeInnsatsgruppe - manglende tilgang - kaster exception`() {
		server.enqueue(MockResponse().setResponseCode(401))
		assertThrows<RuntimeException> { client.hentGjeldendeInnsatsgruppe("123")  }
	}
}
