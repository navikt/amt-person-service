package no.nav.amt.person.service.clients.veilarboppfolging

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import no.nav.amt.person.service.utils.JsonUtils.staticObjectMapper
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.HttpStatus
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.UUID

class VeilarboppfolgingClientTest {
	private lateinit var server: MockWebServer
	private lateinit var client: VeilarboppfolgingClient

	@BeforeEach
	fun setup() {
		server = MockWebServer()
		client =
			VeilarboppfolgingClient(
				apiUrl = server.url("/api").toString(),
				veilarboppfolgingTokenProvider = { "VEILARBOPPFOLGING_TOKEN" },
				objectMapper = staticObjectMapper,
			)
	}

	@Nested
	inner class HentVeilederIdent {
		@Test
		fun `HentVeilederIdent - Skal sende med authorization`() {
			val jsonRepons = """{"veilederIdent":"V123"}""".trimIndent()
			server.enqueue(MockResponse().setBody(jsonRepons))

			client.hentVeilederIdent(FNR_IN_TEST)

			val request = server.takeRequest()

			request.getHeader(AUTHORIZATION) shouldBe "Bearer VEILARBOPPFOLGING_TOKEN"
		}

		@Test
		fun `HentVeilederIdent - Bruker finnes - Returnerer veileder ident`() {
			val jsonRepons = """{"veilederIdent":"V123"}""".trimIndent()
			server.enqueue(MockResponse().setBody(jsonRepons))

			val veileder = client.hentVeilederIdent(FNR_IN_TEST)
			veileder shouldBe VEILEDER_IDENT_IN_TEST
		}

		@Test
		fun `HentVeilederIdent - Manglende tilgang - Kaster exception`() {
			server.enqueue(MockResponse().setResponseCode(HttpStatus.FORBIDDEN.value()))

			val thrown =
				shouldThrow<RuntimeException> {
					client.hentVeilederIdent(FNR_IN_TEST)
				}

			thrown.message shouldStartWith "Uventet status ved kall mot veilarboppfolging"
		}

		@Test
		fun `HentVeilederIdent - Requester korrekt url`() {
			val respons = """{"veilederIdent": "V123"}"""
			server.enqueue(MockResponse().setBody(respons))

			val veilederIdent = client.hentVeilederIdent(FNR_IN_TEST)

			val request = server.takeRequest()

			veilederIdent shouldBe VEILEDER_IDENT_IN_TEST
			request.path shouldBe "/api/api/v3/hent-veileder"
		}

		@Test
		fun `HentVeilederIdent - Bruker finnes ikke - returnerer null`() {
			server.enqueue(MockResponse().setResponseCode(HttpStatus.NO_CONTENT.value()))

			val veilederIdent = client.hentVeilederIdent(FNR_IN_TEST)

			veilederIdent shouldBe null
		}
	}

	@Nested
	inner class HentOppfolgingperioder {
		@Test
		fun `hentOppfolgingperioder - manglende tilgang - kaster exception`() {
			server.enqueue(MockResponse().setResponseCode(HttpStatus.UNAUTHORIZED.value()))

			val thrown =
				shouldThrow<RuntimeException> {
					client.hentOppfolgingperioder(FNR_IN_TEST)
				}

			thrown.message shouldStartWith "Uventet status ved hent status-kall mot veilarboppfolging"
		}

		@ParameterizedTest
		@ValueSource(booleans = [true, false])
		fun `hentOppfolgingperioder - bruker finnes - returnerer oppfolgingsperidoer`(useEndDate: Boolean) {
			val expected = createOppfolgingPeriodeDto(useEndDate)
			server.enqueue(MockResponse().setBody(staticObjectMapper.writeValueAsString(listOf(expected))))

			val oppfolgingsperioder = client.hentOppfolgingperioder(FNR_IN_TEST)

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

		private fun createOppfolgingPeriodeDto(useEndDate: Boolean) =
			VeilarboppfolgingClient.OppfolgingPeriodeDto(
				uuid = UUID.randomUUID(),
				startDato = nowAsZonedDateTimeUtc,
				sluttDato = if (useEndDate) nowAsZonedDateTimeUtc.plusDays(1) else null,
			)
	}
}
