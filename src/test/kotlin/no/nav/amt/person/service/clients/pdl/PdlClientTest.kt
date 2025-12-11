package no.nav.amt.person.service.clients.pdl

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.shouldBe
import no.nav.amt.person.service.clients.pdl.PdlClientTestData.ERROR_PREFIX
import no.nav.amt.person.service.clients.pdl.PdlClientTestData.NULL_ERROR
import no.nav.amt.person.service.clients.pdl.PdlClientTestData.flereFeilRespons
import no.nav.amt.person.service.clients.pdl.PdlClientTestData.fodselsarRespons
import no.nav.amt.person.service.clients.pdl.PdlClientTestData.gyldigRespons
import no.nav.amt.person.service.clients.pdl.PdlClientTestData.minimalFeilRespons
import no.nav.amt.person.service.clients.pdl.PdlClientTestData.telefonResponse
import no.nav.amt.person.service.data.TestData
import no.nav.amt.person.service.integration.IntegrationTestBase
import no.nav.amt.person.service.person.model.IdentType
import no.nav.amt.person.service.person.model.Personident
import no.nav.amt.person.service.poststed.Postnummer
import no.nav.amt.person.service.poststed.PoststedRepository
import no.nav.amt.person.service.utils.JsonUtils.staticObjectMapper
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import java.util.UUID

class PdlClientTest(
	private val poststedRepository: PoststedRepository,
) : IntegrationTestBase() {
	private lateinit var serverUrl: String
	private lateinit var server: MockWebServer

	@BeforeEach
	fun setup() {
		server = MockWebServer()
		serverUrl = server.url("").toString().removeSuffix("/")

		poststedRepository.oppdaterPoststed(
			listOf(
				Postnummer("0484", "OSLO"),
				Postnummer("5341", "STRAUME"),
				Postnummer("5365", "TURØY"),
				Postnummer("5449", "BØMLO"),
				Postnummer("9609", "NORDRE SEILAND"),
			),
			UUID.randomUUID(),
		)
	}

	@AfterEach
	fun cleanup() {
		template.update("DELETE FROM postnummer", MapSqlParameterSource())
		server.shutdown()
	}

	@Test
	fun `hentPerson - gyldig respons - skal lage riktig request og parse pdl person`() {
		val connector =
			PdlClient(
				serverUrl,
				{ "TOKEN" },
				poststedRepository = poststedRepository,
				objectMapper = staticObjectMapper,
			)

		server.enqueue(MockResponse().setBody(gyldigRespons))

		val pdlPerson = connector.hentPerson("FNR")

		pdlPerson.fornavn shouldBe "Tester"
		pdlPerson.mellomnavn shouldBe "Test"
		pdlPerson.etternavn shouldBe "Testersen"
		pdlPerson.telefonnummer shouldBe "+4712345678"
		pdlPerson.adresse
			?.bostedsadresse
			?.matrikkeladresse
			?.tilleggsnavn shouldBe "Storgården"
		pdlPerson.adresse
			?.bostedsadresse
			?.matrikkeladresse
			?.postnummer shouldBe "0484"
		pdlPerson.adresse
			?.bostedsadresse
			?.matrikkeladresse
			?.poststed shouldBe "OSLO"
		pdlPerson.adresse?.oppholdsadresse shouldBe null
		pdlPerson.adresse
			?.kontaktadresse
			?.postboksadresse
			?.postboks shouldBe "Postboks 1234"
		pdlPerson.adresse
			?.bostedsadresse
			?.matrikkeladresse
			?.postnummer shouldBe "0484"
		pdlPerson.adresse
			?.bostedsadresse
			?.matrikkeladresse
			?.poststed shouldBe "OSLO"

		val ident = pdlPerson.identer.first()
		assertSoftly(ident) {
			type shouldBe IdentType.FOLKEREGISTERIDENT
			historisk shouldBe false
			it.ident shouldBe "29119826819"
		}

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
		val client =
			PdlClient(
				serverUrl,
				{ "TOKEN" },
				poststedRepository = poststedRepository,
				objectMapper = staticObjectMapper,
			)

		server.enqueue(
			MockResponse().setBody(
				"""
				{
					"errors": [{"message": "Noe gikk galt"}],
					"data": null
				}
				""".trimIndent(),
			),
		)

		val exception =
			assertThrows<RuntimeException> {
				client.hentPerson("FNR")
			}

		exception.message shouldBe "$ERROR_PREFIX$NULL_ERROR- Noe gikk galt (code: null details: null)\n"

		val request = server.takeRequest()

		request.path shouldBe "/graphql"
		request.method shouldBe "POST"
	}

	@Test
	fun `hentIdenter skal lage riktig request og parse response`() {
		val client =
			PdlClient(
				serverUrl,
				{ "TOKEN" },
				poststedRepository = poststedRepository,
				objectMapper = staticObjectMapper,
			)

		val personident1 = Personident(TestData.randomIdent(), false, IdentType.FOLKEREGISTERIDENT)
		val personident2 = Personident(TestData.randomIdent(), true, IdentType.FOLKEREGISTERIDENT)

		server.enqueue(
			MockResponse().setBody(
				"""
				{
					"errors": null,
					"data": {
						"hentIdenter": {
						  "identer": [
							{
							  "ident": "${personident1.ident}",
							  "historisk": ${personident1.historisk},
							  "gruppe": "${personident1.type.name}"
							},
							{
							  "ident": "${personident2.ident}",
							  "historisk": ${personident2.historisk},
							  "gruppe": "${personident2.type.name}"
							}
						  ]
						}
					  }
				}
				""".trimIndent(),
			),
		)

		val identer = client.hentIdenter(personident2.ident)

		identer shouldBe listOf(personident1, personident2)

		val request = server.takeRequest()

		request.path shouldBe "/graphql"
		request.method shouldBe "POST"
		request.getHeader("Authorization") shouldBe "Bearer TOKEN"
		request.getHeader("Tema") shouldBe "GEN"

		val expectedJson =
			"""
			{
				"query": "${PdlQueries.HentIdenter.query.replace("\n", "\\n").replace("\t", "\\t")}",
				"variables": { "ident": "${personident2.ident}" }
			}
			""".trimIndent()

		val body = request.body.readUtf8()
		body shouldEqualJson expectedJson
	}

	@Test
	fun `hentPerson - Detaljert respons - skal kaste exception med noe detaljert informasjon`() {
		val client =
			PdlClient(
				serverUrl,
				{ "TOKEN" },
				poststedRepository = poststedRepository,
				objectMapper = staticObjectMapper,
			)

		server.enqueue(MockResponse().setBody(minimalFeilRespons))

		val exception =
			assertThrows<RuntimeException> {
				client.hentPerson("FNR")
			}

		exception.message shouldBe ERROR_PREFIX + NULL_ERROR +
			"- Ikke tilgang til å se person (code: unauthorized details: PdlErrorDetails(type=abac-deny, cause=cause-0001-manglerrolle, policy=adressebeskyttelse_strengt_fortrolig_adresse))\n"
	}

	@Test
	fun `hentPerson - Flere feil i respons - skal kaste exception med noe detaljert informasjon`() {
		val client =
			PdlClient(
				serverUrl,
				{ "TOKEN" },
				poststedRepository = poststedRepository,
				objectMapper = staticObjectMapper,
			)

		server.enqueue(MockResponse().setBody(flereFeilRespons))

		val exception =
			assertThrows<RuntimeException> {
				client.hentPerson("FNR")
			}

		exception.message shouldBe ERROR_PREFIX + NULL_ERROR +
			"- Ikke tilgang til å se person (code: unauthorized details: PdlErrorDetails(type=abac-deny, " +
			"cause=cause-0001-manglerrolle, policy=adressebeskyttelse_strengt_fortrolig_adresse))\n" +
			"- Test (code: unauthorized details: PdlErrorDetails(type=abac-deny, cause=cause-0001-manglerrolle, " +
			"policy=adressebeskyttelse_strengt_fortrolig_adresse))\n"
	}

	@Test
	fun `hentTelefon - person har telefon - returnerer telefon`() {
		val client =
			PdlClient(
				serverUrl,
				{ "TOKEN" },
				poststedRepository = poststedRepository,
				objectMapper = staticObjectMapper,
			)

		server.enqueue(MockResponse().setBody(telefonResponse))

		val telefon = client.hentTelefon("FNR")

		telefon shouldBe "+4712345678"
	}

	@Test
	fun `hentPersonFodselsar - person har fodselsar - returnerer fodselsar`() {
		val client =
			PdlClient(
				serverUrl,
				{ "TOKEN" },
				poststedRepository = poststedRepository,
				objectMapper = staticObjectMapper,
			)

		server.enqueue(MockResponse().setBody(fodselsarRespons))

		val fodselsar = client.hentPersonFodselsar("FNR")

		fodselsar shouldBe 1976
	}

	@Test
	fun `hentPersonFodselsar - data mangler - skal kaste exception`() {
		val client =
			PdlClient(
				serverUrl,
				{ "TOKEN" },
				poststedRepository = poststedRepository,
				objectMapper = staticObjectMapper,
			)

		server.enqueue(
			MockResponse().setBody(
				"""
				{
					"errors": [{"message": "Noe gikk galt"}],
					"data": null
				}
				""".trimIndent(),
			),
		)

		val exception =
			assertThrows<RuntimeException> {
				client.hentPersonFodselsar("FNR")
			}

		exception.message shouldBe "$ERROR_PREFIX$NULL_ERROR- Noe gikk galt (code: null details: null)\n"

		val request = server.takeRequest()

		request.path shouldBe "/graphql"
		request.method shouldBe "POST"
	}
}
