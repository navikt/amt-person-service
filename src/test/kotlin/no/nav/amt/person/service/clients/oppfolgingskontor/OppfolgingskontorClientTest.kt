package no.nav.amt.person.service.clients.oppfolgingskontor

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.mockk.every
import no.nav.amt.person.service.clients.RestClientTestBase
import org.hamcrest.CoreMatchers.containsString
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.restclient.test.autoconfigure.RestClientTest
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.client.match.MockRestRequestMatchers.content
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess

@RestClientTest(OppfolgingskontorClient::class)
@TestPropertySource(
    properties = [
        "ao-oppfolgingskontor.url=http://oppfolgingskontor",
        "ao-oppfolgingskontor.scope=test.oppfolgingskontor",
    ],
)
class OppfolgingskontorClientTest(
    @Autowired private val sut: OppfolgingskontorClient,
) : RestClientTestBase() {
    @BeforeEach
    fun setUp() {
        every { tokenClient.createMachineToMachineToken(any()) } returns "OPPFOLGINGSKONTOR_TOKEN"
    }

    @Test
    fun `hentKontorForBruker skal lage riktig request og parse respons`() {
        server
            .expect(requestTo("http://oppfolgingskontor/graphql"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer OPPFOLGINGSKONTOR_TOKEN"))
            .andExpect(content().string(containsString("kontorTilhorigheter")))
            .andExpect(content().string(containsString("12345678901")))
            .andRespond(
                withSuccess(
                    """
                    {
                        "data": {
                            "kontorTilhorigheter": {
                                "arbeidsoppfolging": {
                                    "kontorId": "1234",
                                    "kontorNavn": "NAV Testkontor"
                                }
                            }
                        }
                    }
                    """.trimIndent(),
                    MediaType.APPLICATION_JSON,
                ),
            )

        val kontor = sut.hentKontorForBruker("12345678901").shouldNotBeNull()

        kontor.kontorId shouldBe "1234"
        kontor.kontorNavn shouldBe "NAV Testkontor"
    }

    @Test
    fun `hentKontorForBruker skal returnere null hvis arbeidsoppfolging er null i respons`() {
        server
            .expect(method(HttpMethod.POST))
            .andRespond(
                withSuccess(
                    """
                    {
                        "data": {
                            "kontorTilhorigheter": {
                                "arbeidsoppfolging": null
                            }
                        }
                    }
                    """.trimIndent(),
                    MediaType.APPLICATION_JSON,
                ),
            )

        sut.hentKontorForBruker("12345678901") shouldBe null
    }

    @Test
    fun `hentKontorForBruker skal kaste exception ved GraphQL feil`() {
        server
            .expect(method(HttpMethod.POST))
            .andRespond(
                withSuccess(
                    """
                    {
                        "errors": [
                            {"message": "Bruker ikke funnet"}
                        ]
                    }
                    """.trimIndent(),
                    MediaType.APPLICATION_JSON,
                ),
            )

        val thrown = shouldThrow<RuntimeException> {
            sut.hentKontorForBruker("12345678901")
        }

        thrown.message.shouldStartWith("Feilmeldinger i respons fra ao-oppfolgingskontor")
    }

    @Test
    fun `hentKontorForBruker skal kaste exception ved ikke-vellykket HTTP-status`() {
        server
            .expect(method(HttpMethod.POST))
            .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR))

        shouldThrow<RuntimeException> {
            sut.hentKontorForBruker("12345678901")
        }
    }

    @Test
    fun `hentKontorForBruker skal kaste exception når data er null uten errors`() {
        server
            .expect(method(HttpMethod.POST))
            .andRespond(
                withSuccess(
                    """{ "data": null }""",
                    MediaType.APPLICATION_JSON,
                ),
            )

        val thrown = shouldThrow<RuntimeException> {
            sut.hentKontorForBruker("12345678901")
        }

        thrown.message shouldBe "ao-oppfolgingskontor respons inneholder ikke data"
    }
}
