package no.nav.amt.person.service.clients.amt_tiltak

import io.kotest.matchers.shouldBe
import no.nav.amt.person.service.person.model.IdentType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class AmtTiltakClientTest {
	lateinit var server: MockWebServer
	lateinit var client: AmtTiltakClient

	@BeforeEach
	fun setup() {
		server = MockWebServer()
		client = AmtTiltakClient(
			baseUrl = server.url("").toString().removeSuffix("/"),
			tokenProvider = { "TOKEN" },
		)
	}

	@Test
	fun `hentBrukerInfo - bruker finnes - returnerer brukerId og navEnhetId`() {
		val brukerId = UUID.randomUUID()
		val deltakerId = UUID.randomUUID()
		val navEnhetId = UUID.randomUUID()

		val response = MockResponse().setBody("""{
				"brukerId": "$brukerId",
				"navEnhetId": "$navEnhetId",
				"personIdentType": "FOLKEREGISTERIDENT",
				"historiskeIdenter": []
			}""".trimMargin())

		server.enqueue(response)

		val info = client.hentBrukerInfo(deltakerId)
		info.brukerId shouldBe brukerId
		info.navEnhetId shouldBe navEnhetId
		info.historiskeIdenter shouldBe emptyList()
		info.personIdentType shouldBe IdentType.FOLKEREGISTERIDENT

		val request = server.takeRequest()

		request.path shouldBe "/api/tiltaksarrangor/deltaker/$deltakerId/bruker-info"
		request.method shouldBe "GET"
		request.getHeader("Authorization") shouldBe "Bearer TOKEN"
	}
}
