package no.nav.amt.person.service.clients.pdl

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.shouldBe
import no.nav.amt.person.service.clients.pdl.PdlClientTestData.errorPrefix
import no.nav.amt.person.service.clients.pdl.PdlClientTestData.flereFeilRespons
import no.nav.amt.person.service.clients.pdl.PdlClientTestData.gyldigRespons
import no.nav.amt.person.service.clients.pdl.PdlClientTestData.minimalFeilRespons
import no.nav.amt.person.service.clients.pdl.PdlClientTestData.nullError
import no.nav.amt.person.service.clients.pdl.PdlClientTestData.telefonResponse
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class PdlClientTest {
	private lateinit var serverUrl: String
	private lateinit var server: MockWebServer

	@BeforeEach
	fun setup() {
		server = MockWebServer()
		serverUrl = server.url("").toString().removeSuffix("/")
	}

	@AfterEach
	fun cleanup() {
		server.shutdown()
	}

	@Test
	fun `hentPerson - gyldig respons - skal lage riktig request og parse pdl person`() {
		val connector = PdlClient(
			serverUrl,
			{ "TOKEN" },
		)

		server.enqueue(MockResponse().setBody(gyldigRespons))

		val pdlPerson = connector.hentPerson("FNR")

		pdlPerson.fornavn shouldBe "Tester"
		pdlPerson.mellomnavn shouldBe "Test"
		pdlPerson.etternavn shouldBe "Testersen"
		pdlPerson.telefonnummer shouldBe "+47 12345678"

		val ident = pdlPerson.identer.first()
		ident.gruppe shouldBe "FOLKEREGISTERIDENT"
		ident.historisk shouldBe false
		ident.ident shouldBe "29119826819"

		val request = server.takeRequest()

		request.path shouldBe "/graphql"
		request.method shouldBe "POST"
		request.getHeader("Authorization") shouldBe "Bearer TOKEN"
		request.getHeader("Tema") shouldBe "GEN"

		val expectedJson =
			"""
				{
					"query": "${PdlQueries.HentPerson.query.replace("\n", "\\n").replace("\t", "\\t")}",
					"variables": { "ident": "FNR" }
				}
			""".trimIndent()

		val body = request.body.readUtf8()
		body shouldEqualJson expectedJson
	}

	@Test
	fun `hentPerson - data mangler - skal kaste exception`() {
		val client = PdlClient(
			serverUrl,
			{ "TOKEN" },
		)

		server.enqueue(
			MockResponse().setBody(
				"""
					{
						"errors": [{"message": "Noe gikk galt"}],
						"data": null
					}
				""".trimIndent()
			)
		)

		val exception = assertThrows<RuntimeException> {
			client.hentPerson("FNR")
		}

		exception.message shouldBe "$errorPrefix$nullError- Noe gikk galt (code: null details: null)\n"

		val request = server.takeRequest()

		request.path shouldBe "/graphql"
		request.method shouldBe "POST"
	}

	@Test
	fun `hentGjeldendePersonligIdent skal lage riktig request og parse response`() {
		val client = PdlClient(
			serverUrl,
			{ "TOKEN" },
		)

		server.enqueue(
			MockResponse().setBody(
				"""
					{
						"errors": null,
						"data": {
							"hentIdenter": {
							  "identer": [
								{
								  "ident": "12345678900"
								}
							  ]
							}
						  }
					}
				""".trimIndent()
			)
		)

		val gjeldendeIdent = client.hentGjeldendePersonligIdent("112233445566")

		gjeldendeIdent shouldBe "12345678900"

		val request = server.takeRequest()

		request.path shouldBe "/graphql"
		request.method shouldBe "POST"
		request.getHeader("Authorization") shouldBe "Bearer TOKEN"
		request.getHeader("Tema") shouldBe "GEN"

		val expectedJson =
			"""
				{
					"query": "${PdlQueries.HentGjeldendeIdent.query.replace("\n", "\\n").replace("\t", "\\t")}",
					"variables": { "ident": "112233445566" }
				}
			""".trimIndent()

		val body = request.body.readUtf8()
		body shouldEqualJson expectedJson
	}

	@Test
	fun `hentGjeldendePersonligIdent skal kaste exception hvis data mangler`() {
		val client = PdlClient(
			serverUrl,
			{ "TOKEN" },
		)

		server.enqueue(
			MockResponse().setBody(
				"""
					{
						"errors": [{"message": "error :("}],
						"data": null
					}
				""".trimIndent()
			)
		)

		val exception = assertThrows<RuntimeException> {
			client.hentGjeldendePersonligIdent("112233445566")
		}

		exception.message shouldBe "PDL respons inneholder ikke data"

		val request = server.takeRequest()

		request.path shouldBe "/graphql"
		request.method shouldBe "POST"
		request.getHeader("Authorization") shouldBe "Bearer TOKEN"
		request.getHeader("Tema") shouldBe "GEN"
	}

	@Test
	fun `hentPerson - Detaljert respons - skal kaste exception med noe detaljert informasjon`() {
		val client = PdlClient(
			serverUrl,
			{ "TOKEN" },
		)

		server.enqueue(MockResponse().setBody(minimalFeilRespons))

		val exception = assertThrows<RuntimeException> {
			client.hentPerson("FNR")
		}

		exception.message shouldBe errorPrefix + nullError +
			"- Ikke tilgang til å se person (code: unauthorized details: PdlErrorDetails(type=abac-deny, cause=cause-0001-manglerrolle, policy=adressebeskyttelse_strengt_fortrolig_adresse))\n"
	}

	@Test
	fun `hentPerson - Flere feil i respons - skal kaste exception med noe detaljert informasjon`() {
		val client = PdlClient(
			serverUrl,
			{ "TOKEN" },
		)

		server.enqueue(MockResponse().setBody(flereFeilRespons))

		val exception = assertThrows<RuntimeException> {
			client.hentPerson("FNR")
		}

		exception.message shouldBe errorPrefix + nullError +
			"- Ikke tilgang til å se person (code: unauthorized details: PdlErrorDetails(type=abac-deny, cause=cause-0001-manglerrolle, policy=adressebeskyttelse_strengt_fortrolig_adresse))\n" +
			"- Test (code: unauthorized details: PdlErrorDetails(type=abac-deny, cause=cause-0001-manglerrolle, policy=adressebeskyttelse_strengt_fortrolig_adresse))\n"
	}

	@Test
	fun `hentTelefon - person har telefon - returnerer telefon`() {
		val client = PdlClient(
			serverUrl,
			{ "TOKEN" },
		)

		server.enqueue(MockResponse().setBody(telefonResponse))

		val telefon = client.hentTelefon("FNR")

		telefon shouldBe "+47 12345678"
	}

}
