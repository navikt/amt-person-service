package no.nav.amt.person.service.clients

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.hamcrest.CoreMatchers.containsString
import org.junit.jupiter.api.Test
import org.springframework.boot.restclient.test.autoconfigure.RestClientTest
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess

@RestClientTest(KodeverkClient::class)
class KodeverkClientTest(
    private val sut: KodeverkClient,
) : RestClientTestBase("kodeverk-api") {
    @Test
    fun `hentKodeverk - skal sende riktige headere og query-parametre, og parse respons`() {
        server
            .expect(requestTo(containsString("http://kodeverk-api/api/v1/kodeverk/Postnummer/koder/betydninger")))
            .andExpect(requestTo(containsString("ekskluderUgyldige=true")))
            .andExpect(requestTo(containsString("oppslagsdato=")))
            .andExpect(requestTo(containsString("spraak=nb")))
            .andExpect(method(HttpMethod.GET))
            .andExpect(header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
            .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer kodeverk-api-token"))
            .andExpect(header(NAV_CONSUMER_ID_HEADER, NAV_CONSUMER_ID_HEADER_VALUE))
            .andRespond(
                withSuccess(
                    javaClass.getResourceAsStream("/kodeverkrespons.json")!!.bufferedReader().readText(),
                    MediaType.APPLICATION_JSON,
                ),
            )

        val postnummer = sut.hentKodeverk()

        postnummer.find { it.postnummer == "3831" }?.poststed shouldBe "ULEFOSS"
    }

    @Test
    fun `hentKodeverk - 500 fra kodeverk - kaster RuntimeException`() {
        server
            .expect(method(HttpMethod.GET))
            .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR))

        shouldThrow<RuntimeException> {
            sut.hentKodeverk()
        }
    }

    @Test
    fun `hentKodeverk - tomt svar fra kodeverk - kaster RuntimeException`() {
        server
            .expect(method(HttpMethod.GET))
            .andRespond(withSuccess("", MediaType.APPLICATION_JSON))

        shouldThrow<RuntimeException> {
            sut.hentKodeverk()
        }
    }
}
