package no.nav.amt.person.service.clients.norg

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.restclient.test.autoconfigure.RestClientTest
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess

@RestClientTest(NorgClient::class)
@TestPropertySource(properties = ["norg.url=http://norg"])
class NorgClientTest(
    @Autowired private val sut: NorgClient,
    @Autowired private val server: MockRestServiceServer,
) {
    @Test
    fun `hentNavEnhet - skal lage riktig request og parse respons`() {
        server
            .expect(requestTo("http://norg/norg2/api/v1/enhet/1234"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(
                withSuccess(
                    """
                    {
                      "navn": "NAV Testheim",
                      "enhetNr": "1234"
                    }
                    """.trimIndent(),
                    MediaType.APPLICATION_JSON,
                ),
            )

        val enhet = sut.hentNavEnhet("1234")

        enhet?.enhetNr shouldBe "1234"
        enhet?.navn shouldBe "NAV Testheim"
    }

    @Test
    fun `hentNavEnhet - 404 fra norg - returnerer null`() {
        server
            .expect(requestTo("http://norg/norg2/api/v1/enhet/4321"))
            .andRespond(withStatus(HttpStatus.NOT_FOUND))

        sut.hentNavEnhet("4321").shouldBeNull()
    }

    @Test
    fun `hentNavEnhet - 500 fra norg - kaster exception`() {
        server
            .expect(requestTo("http://norg/norg2/api/v1/enhet/9999"))
            .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR))

        shouldThrow<RuntimeException> {
            sut.hentNavEnhet("9999")
        }
    }

    @Test
    fun `hentNavEnhet - ugyldig enhetId - kaster IllegalArgumentException`() {
        shouldThrow<IllegalArgumentException> {
            sut.hentNavEnhet("12")
        }
    }

    @Test
    fun `hentNavEnheter - skal lage riktig request og parse respons`() {
        server
            .expect(requestTo("http://norg/norg2/api/v1/enhet?enhetsnummerListe=1234,5678"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(
                withSuccess(
                    """
                    [
                      { "navn": "NAV Testheim", "enhetNr": "1234" },
                      { "navn": "NAV Annet",    "enhetNr": "5678" }
                    ]
                    """.trimIndent(),
                    MediaType.APPLICATION_JSON,
                ),
            )

        val enheter = sut.hentNavEnheter(listOf("1234", "5678"))

        enheter shouldBe listOf(
            NorgNavEnhetDto(navn = "NAV Testheim", enhetNr = "1234"),
            NorgNavEnhetDto(navn = "NAV Annet", enhetNr = "5678"),
        )
    }

    @Test
    fun `hentNavEnheter - 500 fra norg - kaster exception`() {
        server
            .expect(method(HttpMethod.GET))
            .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR))

        shouldThrow<RuntimeException> {
            sut.hentNavEnheter(listOf("1234"))
        }
    }

    @Test
    fun `hentNavEnheter - ugyldig enhetId - kaster IllegalArgumentException`() {
        shouldThrow<IllegalArgumentException> {
            sut.hentNavEnheter(listOf("12"))
        }
    }
}
