package no.nav.amt.person.service.clients.veilarbarena

import io.kotest.matchers.shouldBe
import no.nav.amt.person.service.utils.JsonUtils.staticObjectMapper
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus

class VeilarbarenaClientTest {
	lateinit var server: MockWebServer
	lateinit var client: VeilarbarenaClient

	@BeforeEach
	fun setup() {
		server = MockWebServer()
		client =
			VeilarbarenaClient(
				baseUrl = server.url("").toString().removeSuffix("/"),
				tokenProvider = { "VEILARBARENA_TOKEN" },
				objectMapper = staticObjectMapper,
			)
	}

	@Test
	fun `hentBrukerOppfolgingsenhetId skal lage riktig request og parse respons`() {
		server.enqueue(
			MockResponse().setBody(
				"""
				{
					"formidlingsgruppe": "ARBS",
					"kvalifiseringsgruppe": "BFORM",
					"rettighetsgruppe": "DAGP",
					"iservFraDato": "2021-11-16T10:09:03",
					"oppfolgingsenhet": "1234"
				}
				""".trimIndent(),
			),
		)

		val oppfolgingsenhetId = client.hentBrukerOppfolgingsenhetId("987654")

		oppfolgingsenhetId shouldBe "1234"

		val request = server.takeRequest()

		request.path shouldBe "/veilarbarena/api/v2/arena/hent-status"
		request.method shouldBe "POST"
		request.getHeader(HttpHeaders.AUTHORIZATION) shouldBe "Bearer VEILARBARENA_TOKEN"
		request.getHeader("Nav-Consumer-Id") shouldBe "amt-person-service"
	}

	@Test
	fun `hentBrukerOppfolgingsenhetId skal returnere null hvis veilarbarena returnerer 404`() {
		server.enqueue(MockResponse().setResponseCode(HttpStatus.NOT_FOUND.value()))

		client.hentBrukerOppfolgingsenhetId("987654") shouldBe null
	}
}
