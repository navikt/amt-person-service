package no.nav.amt.person.service.clients

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.mockk.every
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
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
import tools.jackson.databind.ObjectMapper
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.UUID

@RestClientTest(VeilarboppfolgingClient::class)
@TestPropertySource(
    properties = [
        "veilarboppfolging.url=http://veilarboppfolging-host",
        "veilarboppfolging.scope=test.veilarboppfolging",
    ],
)
class VeilarboppfolgingClientTest(
    @Autowired private val sut: VeilarboppfolgingClient,
    @Autowired private val objectMapper: ObjectMapper,
) : RestClientTestBase() {
    @Nested
    inner class HentVeilederIdent {
        @BeforeEach
        fun setUp() {
            every { tokenClient.createMachineToMachineToken(any()) } returns "VEILARBOPPFOLGING_TOKEN"
        }

        @Test
        fun `HentVeilederIdent - Skal sende med authorization og treffe riktig URL`() {
            server
                .expect(requestTo("http://veilarboppfolging-host/veilarboppfolging/api/v3/hent-veileder"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer VEILARBOPPFOLGING_TOKEN"))
                .andExpect(content().json("""{"fnr":"$FNR_IN_TEST"}"""))
                .andRespond(
                    withSuccess(
                        """{"veilederIdent":"V123"}""",
                        MediaType.APPLICATION_JSON,
                    ),
                )

            sut.hentVeilederIdent(FNR_IN_TEST)
        }

        @Test
        fun `HentVeilederIdent - Bruker finnes - Returnerer veileder ident`() {
            server
                .expect(method(HttpMethod.POST))
                .andRespond(
                    withSuccess(
                        """{"veilederIdent":"V123"}""",
                        MediaType.APPLICATION_JSON,
                    ),
                )

            sut.hentVeilederIdent(FNR_IN_TEST) shouldBe VEILEDER_IDENT_IN_TEST
        }

        @Test
        fun `HentVeilederIdent - Manglende tilgang - Kaster exception`() {
            server
                .expect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.FORBIDDEN))

            val thrown = shouldThrow<RuntimeException> {
                sut.hentVeilederIdent(FNR_IN_TEST)
            }

            thrown.message shouldStartWith "Uventet status ved kall mot veilarboppfolging"
        }

        @Test
        fun `HentVeilederIdent - Bruker finnes ikke - returnerer null`() {
            server
                .expect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.NO_CONTENT))

            sut.hentVeilederIdent(FNR_IN_TEST) shouldBe null
        }
    }

    @Nested
    inner class HentOppfolgingperioder {
        @Test
        fun `hentOppfolgingperioder - manglende tilgang - kaster exception`() {
            server
                .expect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED))

            val thrown = shouldThrow<RuntimeException> {
                sut.hentOppfolgingperioder(FNR_IN_TEST)
            }

            thrown.message shouldStartWith "Uventet status ved hent status-kall mot veilarboppfolging"
        }

        @Test
        fun `hentOppfolgingperioder - tomt svar - returnerer tom liste`() {
            server
                .expect(method(HttpMethod.POST))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON))

            sut.hentOppfolgingperioder(FNR_IN_TEST) shouldBe emptyList()
        }

        @ParameterizedTest
        @ValueSource(booleans = [true, false])
        fun `hentOppfolgingperioder - bruker finnes - returnerer oppfolgingsperidoer`(useEndDate: Boolean) {
            val expected = createOppfolgingPeriodeDto(useEndDate)

            server
                .expect(
                    requestTo("http://veilarboppfolging-host/veilarboppfolging/api/v3/oppfolging/hent-perioder"),
                ).andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer $TOKEN_IN_TEST"))
                .andExpect(content().json("""{"fnr":"$FNR_IN_TEST"}"""))
                .andRespond(
                    withSuccess(
                        objectMapper.writeValueAsString(listOf(expected)),
                        MediaType.APPLICATION_JSON,
                    ),
                )

            val oppfolgingsperioder = sut.hentOppfolgingperioder(FNR_IN_TEST)

            oppfolgingsperioder.size shouldBe 1
            assertSoftly(oppfolgingsperioder.first()) {
                id shouldBe expected.uuid
                startdato shouldBe nowAsLocalDateTime

                if (useEndDate) {
                    sluttdato shouldBe nowAsLocalDateTime.plusDays(1)
                } else {
                    sluttdato shouldBe null
                }
            }
        }
    }

    companion object {
        private const val VEILEDER_IDENT_IN_TEST = "V123"
        private const val FNR_IN_TEST = "123"

        private val nowAsZonedDateTimeUtc: ZonedDateTime = ZonedDateTime.now(ZoneOffset.UTC)
        private val nowAsLocalDateTime: LocalDateTime =
            nowAsZonedDateTimeUtc
                .withZoneSameInstant(ZoneId.systemDefault())
                .toLocalDateTime()

        private fun createOppfolgingPeriodeDto(useEndDate: Boolean) = VeilarboppfolgingClient.OppfolgingPeriodeDto(
            uuid = UUID.randomUUID(),
            startDato = nowAsZonedDateTimeUtc,
            sluttDato = if (useEndDate) nowAsZonedDateTimeUtc.plusDays(1) else null,
        )
    }
}
