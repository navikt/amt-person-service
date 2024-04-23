package no.nav.amt.person.service.clients.veilarboppfolging

import io.kotest.matchers.shouldBe
import no.nav.amt.person.service.utils.JsonUtils.toJsonString
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.ZonedDateTime
import java.util.UUID

class VeilarboppfolgingClientTest {
    private lateinit var server: MockWebServer
    private lateinit var client: VeilarboppfolgingClient

	private val veilederIdent = "V123"
	private val fnr = "123"

	@BeforeEach
    fun setup() {
        server = MockWebServer()
		val serverUrl = server.url("/api").toString()
        client = VeilarboppfolgingClient(
			apiUrl = serverUrl,
			veilarboppfolgingTokenProvider = { "VEILARBOPPFOLGING_TOKEN" }
		)
	}

	@Test
	fun `HentVeilederIdent - Skal sende med authorization`() {
		val jsonRepons = """{"veilederIdent":"V123"}""".trimIndent()
		server.enqueue(MockResponse().setBody(jsonRepons))

		client.hentVeilederIdent(fnr)

		val request = server.takeRequest()

		request.getHeader("Authorization") shouldBe "Bearer VEILARBOPPFOLGING_TOKEN"
	}

	@Test
    fun `HentVeilederIdent - Bruker finnes - Returnerer veileder ident`() {
		val jsonRepons = """{"veilederIdent":"V123"}""".trimIndent()
		server.enqueue(MockResponse().setBody(jsonRepons))

        val veileder = client.hentVeilederIdent(fnr)
		veileder shouldBe veilederIdent
    }

	@Test
    fun `HentVeilederIdent - Manglende tilgang - Kaster exception`() {
		server.enqueue(MockResponse().setResponseCode(401))
		assertThrows<RuntimeException> { client.hentVeilederIdent("123")  }
    }

	@Test
	fun `HentVeilederIdent - Requester korrekt url`() {
		val respons = """{"veilederIdent": "V123"}"""

		server.enqueue(MockResponse().setBody(respons))
		client.hentVeilederIdent(fnr)

		val request = server.takeRequest()

		request.path shouldBe "/api/api/v3/hent-veileder"
	}

	@Test
	fun `HentVeilederIdent - Bruker finnes ikke - returnerer null`() {
		server.enqueue(MockResponse().setResponseCode(204))
		val veileder = client.hentVeilederIdent(fnr)

		veileder shouldBe null
	}

	@Test
	fun `hentOppfolgingperioder - bruker finnes - returnerer oppfolgingsperidoer`() {
		val id = UUID.randomUUID()
		val startdato = ZonedDateTime.now()
		val oppfolgingsperioderRespons = listOf(
			VeilarboppfolgingClient.OppfolgingPeriodeDTO(
				uuid = id,
				startDato = startdato,
				sluttDato = null
			)
		)
		server.enqueue(MockResponse().setBody(toJsonString(oppfolgingsperioderRespons)))

		val oppfolgingsperioder = client.hentOppfolgingperioder(fnr)
		oppfolgingsperioder.size shouldBe 1
		oppfolgingsperioder.first().id shouldBe id
		oppfolgingsperioder.first().startdato.toLocalDate() shouldBe startdato.toLocalDate()
		oppfolgingsperioder.first().sluttdato shouldBe null
	}

	@Test
	fun `hentOppfolgingperioder - manglende tilgang - kaster exception`() {
		server.enqueue(MockResponse().setResponseCode(401))
		assertThrows<RuntimeException> { client.hentOppfolgingperioder("123")  }
	}
}
