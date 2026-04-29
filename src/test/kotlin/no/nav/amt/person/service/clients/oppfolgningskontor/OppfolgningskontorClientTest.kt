package no.nav.amt.person.service.clients.oppfolgningskontor

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.amt.person.service.utils.JsonUtils.staticObjectMapper
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus

class OppfolgningskontorClientTest {
    lateinit var server: MockWebServer
    lateinit var client: OppfolgningskontorClient

    @BeforeEach
    fun setup() {
        server = MockWebServer()
        client =
            OppfolgningskontorClient(
                baseUrl = server.url("").toString().removeSuffix("/"),
                tokenProvider = { "OPPFOLGNINGSKONTOR_TOKEN" },
                objectMapper = staticObjectMapper,
            )
    }

    @Test
    fun `hentKontorForBruker skal lage riktig request og parse respons`() {
        server.enqueue(
            MockResponse().setBody(
                """
                {
                    "data": {
                        "kontorForBruker": {
                            "enhetId": "1234",
                            "navn": "NAV Testkontor"
                        }
                    }
                }
                """.trimIndent(),
            ),
        )

        val kontor = client.hentKontorForBruker("12345678901")

        kontor?.enhetId shouldBe "1234"
        kontor?.navn shouldBe "NAV Testkontor"

        val request = server.takeRequest()
        val requestBody = request.body.readUtf8()

        request.path shouldBe "/graphql"
        request.method shouldBe HttpMethod.POST.name()
        request.getHeader(HttpHeaders.AUTHORIZATION) shouldBe "Bearer OPPFOLGNINGSKONTOR_TOKEN"
        requestBody.contains("kontorForBruker") shouldBe true
        requestBody.contains("12345678901") shouldBe true
    }

    @Test
    fun `hentKontorForBruker skal returnere null hvis kontorForBruker er null i respons`() {
        server.enqueue(
            MockResponse().setBody(
                """
                {
                    "data": {
                        "kontorForBruker": null
                    }
                }
                """.trimIndent(),
            ),
        )

        val kontor = client.hentKontorForBruker("12345678901")

        kontor shouldBe null
    }

    @Test
    fun `hentKontorForBruker skal kaste exception ved GraphQL feil`() {
        server.enqueue(
            MockResponse().setBody(
                """
                {
                    "errors": [
                        {"message": "Bruker ikke funnet"}
                    ]
                }
                """.trimIndent(),
            ),
        )

        shouldThrow<RuntimeException> {
            client.hentKontorForBruker("12345678901")
        }.message?.contains("Feilmeldinger i respons fra ao-oppfolgingskontor") shouldBe true
    }

    @Test
    fun `hentKontorForBruker skal kaste exception ved ikke-vellykket HTTP-status`() {
        server.enqueue(MockResponse().setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR.value()))

        shouldThrow<RuntimeException> {
            client.hentKontorForBruker("12345678901")
        }.message?.contains("Status: 500") shouldBe true
    }
}
