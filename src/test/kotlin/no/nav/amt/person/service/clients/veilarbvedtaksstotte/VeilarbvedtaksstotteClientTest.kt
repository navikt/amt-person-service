package no.nav.amt.person.service.clients.veilarbvedtaksstotte

import io.kotest.matchers.shouldBe
import no.nav.amt.person.service.navbruker.InnsatsgruppeV1
import no.nav.amt.person.service.navbruker.InnsatsgruppeV2
import no.nav.amt.person.service.utils.JsonUtils.staticObjectMapper
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus

class VeilarbvedtaksstotteClientTest {
	private lateinit var server: MockWebServer
	private lateinit var client: VeilarbvedtaksstotteClient

	private val fnr = "123"

	@BeforeEach
	fun setup() {
		server = MockWebServer()
		val serverUrl = server.url("/api").toString()
		client =
			VeilarbvedtaksstotteClient(
				apiUrl = serverUrl,
				veilarbvedtaksstotteTokenProvider = { "VEILARBVEDTAKSSTOTTE_TOKEN" },
				objectMapper = staticObjectMapper,
			)
	}

	@Test
	fun `hentInnsatsgruppe - bruker har innsatsgruppe - returnerer innsatsgruppe`() {
		val siste14aVedtakDTORespons =
			VeilarbvedtaksstotteClient.Gjeldende14aVedtakResponse(
				innsatsgruppe = InnsatsgruppeV2.JOBBE_DELVIS,
			)
		server.enqueue(MockResponse().setBody(staticObjectMapper.writeValueAsString(siste14aVedtakDTORespons)))

		val innsatsgruppe = client.hentInnsatsgruppe(fnr)

		innsatsgruppe shouldBe InnsatsgruppeV1.GRADERT_VARIG_TILPASSET_INNSATS
	}

	@Test
	fun `hentInnsatsgruppe - bruker har ikke innsatsgruppe - returnerer null`() {
		server.enqueue(MockResponse())

		val innsatsgruppe = client.hentInnsatsgruppe(fnr)

		innsatsgruppe shouldBe null
	}

	@Test
	fun `hentInnsatsgruppe - manglende tilgang - kaster exception`() {
		server.enqueue(MockResponse().setResponseCode(HttpStatus.FORBIDDEN.value()))
		assertThrows<RuntimeException> { client.hentInnsatsgruppe("123") }
	}
}
