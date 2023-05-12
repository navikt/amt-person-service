package no.nav.amt.person.service.clients.krr

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class KrrProxyClientTest {
	private lateinit var server: MockWebServer
	private lateinit var serverUrl: String

	@BeforeEach
	fun setup() {
		server = MockWebServer()
		serverUrl = server.url("").toString().removeSuffix("/")
	}

	@AfterEach
	fun cleanup() {
		server.shutdown()
	}

	@Test
	fun `hentKontaktinformasjon skal lage riktig request og parse respons`() {
		val client = KrrProxyClient(
			baseUrl = serverUrl,
			tokenProvider = { "TOKEN" },
		)

		server.enqueue(
			MockResponse().setBody(
				"""
						{
						  "personident": "12345678900",
						  "kanVarsles": true,
						  "reservert": false,
						  "epostadresse": "testbruker@gmail.test",
						  "mobiltelefonnummer": "11111111"
						}
					""".trimIndent()
			)
		)

		val kontaktinformasjon = client.hentKontaktinformasjon("12345678900").getOrThrow()

		kontaktinformasjon.epost shouldBe "testbruker@gmail.test"
		kontaktinformasjon.telefonnummer shouldBe "11111111"

		val request = server.takeRequest()

		request.path shouldBe "/rest/v1/person?inkluderSikkerDigitalPost=false"
		request.method shouldBe "GET"
		request.getHeader("Authorization") shouldBe "Bearer TOKEN"
		request.getHeader("Nav-Personident") shouldBe "12345678900"
		request.getHeader("Nav-Call-Id") shouldNotBe null
	}

}
