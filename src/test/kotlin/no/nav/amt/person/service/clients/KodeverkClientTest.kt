package no.nav.amt.person.service.clients

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.restclient.test.autoconfigure.RestClientTest
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.client.match.MockRestRequestMatchers
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import java.time.LocalDate
import java.util.UUID

@RestClientTest(KodeverkClient::class)
@TestPropertySource(
    properties = [
        "kodeverk.url=http://kodeverk",
        "kodeverk.scope=test.kodeverk",
    ],
)
class KodeverkClientTest(
    @Autowired private val sut: KodeverkClient,
) : RestClientTestBase() {
    @Test
    fun `hentKodeverk - skal sende riktige headere og query-parametre, og parse respons`() {
        val callId = UUID.randomUUID()

        server
            .expect(
                MockRestRequestMatchers.requestToUriTemplate(
                    "http://kodeverk/api/v1/kodeverk/Postnummer/koder/betydninger?ekskluderUgyldige=true&oppslagsdato={d}&spraak=nb",
                    LocalDate.now(),
                ),
            ).andExpect(method(HttpMethod.GET))
            .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-token"))
            .andExpect(header("Nav-Call-Id", callId.toString()))
            .andExpect(header("Nav-Consumer-Id", "amt-person-service"))
            .andRespond(
                withSuccess(
                    javaClass.getResourceAsStream("/kodeverkrespons.json")!!.bufferedReader().readText(),
                    MediaType.APPLICATION_JSON,
                ),
            )

        val postnummer = sut.hentKodeverk(callId)

        postnummer.find { it.postnummer == "3831" }?.poststed shouldBe "ULEFOSS"
    }

    @Test
    fun `hentKodeverk - 500 fra kodeverk - kaster RuntimeException`() {
        server
            .expect(method(HttpMethod.GET))
            .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR))

        shouldThrow<RuntimeException> {
            sut.hentKodeverk(UUID.randomUUID())
        }
    }

    @Test
    fun `hentKodeverk - tomt svar fra kodeverk - kaster RuntimeException`() {
        server
            .expect(method(HttpMethod.GET))
            .andRespond(withSuccess("", MediaType.APPLICATION_JSON))

        shouldThrow<RuntimeException> {
            sut.hentKodeverk(UUID.randomUUID())
        }
    }
}
