package no.nav.amt.person.service.clients

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.amt.person.service.navbruker.InnsatsgruppeV1
import no.nav.amt.person.service.navbruker.InnsatsgruppeV2
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.restclient.test.autoconfigure.RestClientTest
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
import tools.jackson.databind.ObjectMapper

@RestClientTest(VeilarbvedtaksstotteClient::class)
@TestPropertySource(
    properties = [
        "veilarbvedtaksstotte.url=http://veilarbvedtaksstotte-host",
        "veilarbvedtaksstotte.scope=test.veilarbvedtaksstotte",
    ],
)
class VeilarbvedtaksstotteClientTest(
    @Autowired private val sut: VeilarbvedtaksstotteClient,
    @Autowired private val objectMapper: ObjectMapper,
) : RestClientTestBase() {
    @Test
    fun `hentInnsatsgruppe - bruker har innsatsgruppe - returnerer innsatsgruppe`() {
        val responseBody = objectMapper.writeValueAsString(
            VeilarbvedtaksstotteClient.Gjeldende14aVedtakResponse(
                innsatsgruppe = InnsatsgruppeV2.JOBBE_DELVIS,
            ),
        )

        server
            .expect(
                requestTo("http://veilarbvedtaksstotte-host/veilarbvedtaksstotte/api/hent-gjeldende-14a-vedtak"),
            ).andExpect(method(HttpMethod.POST))
            .andExpect(header("Authorization", "Bearer $TOKEN_IN_TEST"))
            .andExpect(content().json("""{"fnr":"$FNR_IN_TEST"}"""))
            .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON))

        sut.hentInnsatsgruppe(FNR_IN_TEST) shouldBe InnsatsgruppeV1.GRADERT_VARIG_TILPASSET_INNSATS
    }

    @Test
    fun `hentInnsatsgruppe - bruker har ikke innsatsgruppe - returnerer null`() {
        server
            .expect(method(HttpMethod.POST))
            .andRespond(withSuccess("", MediaType.APPLICATION_JSON))

        sut.hentInnsatsgruppe(FNR_IN_TEST) shouldBe null
    }

    @Test
    fun `hentInnsatsgruppe - manglende tilgang - kaster exception`() {
        server
            .expect(method(HttpMethod.POST))
            .andRespond(withStatus(HttpStatus.FORBIDDEN))

        shouldThrow<RuntimeException> {
            sut.hentInnsatsgruppe(FNR_IN_TEST)
        }
    }

    companion object {
        private const val FNR_IN_TEST = "123"
    }
}
