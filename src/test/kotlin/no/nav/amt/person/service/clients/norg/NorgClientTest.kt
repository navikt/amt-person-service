package no.nav.amt.person.service.clients.norg

import io.kotest.matchers.shouldBe
import no.nav.amt.person.service.utils.JsonUtils.staticObjectMapper
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class NorgClientTest {
	private lateinit var server: MockWebServer
	private lateinit var client: NorgClient

	@BeforeEach
	fun setup() {
		server = MockWebServer()
		client =
			NorgClient(
				url = server.url("").toString().removeSuffix("/"),
				objectMapper = staticObjectMapper,
			)
	}

	@AfterEach
	fun cleanup() = server.shutdown()

	@Test
	fun `hentNavEnhetNavn skal lage riktig request og parse respons`() {
		server.enqueue(
			MockResponse().setBody(
				"""
				{
				  "enhetId": 900000042,
				  "navn": "NAV Testheim",
				  "enhetNr": "1234",
				  "antallRessurser": 330,
				  "status": "Aktiv",
				  "orgNivaa": "EN",
				  "type": "LOKAL",
				  "organisasjonsnummer": "12345645",
				  "underEtableringDato": "1970-01-01",
				  "aktiveringsdato": "1970-01-01",
				  "underAvviklingDato": null,
				  "nedleggelsesdato": null,
				  "oppgavebehandler": true,
				  "versjon": 40,
				  "sosialeTjenester": "Fritekst",
				  "kanalstrategi": null,
				  "orgNrTilKommunaltNavKontor": "123456789"
				}
				""".trimIndent(),
			),
		)

		val enhet = client.hentNavEnhet("1234")

		enhet?.enhetId shouldBe "1234"
		enhet?.navn shouldBe "NAV Testheim"

		val request = server.takeRequest()

		request.path shouldBe "/norg2/api/v1/enhet/1234"
	}
}
