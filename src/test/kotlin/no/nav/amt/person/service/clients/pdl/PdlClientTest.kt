package no.nav.amt.person.service.clients.pdl

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.shouldBe
import no.nav.amt.person.service.clients.HeaderConstants.GEN_TEMA_HEADER_VALUE
import no.nav.amt.person.service.clients.HeaderConstants.TEMA_HEADER
import no.nav.amt.person.service.clients.pdl.PdlClientTestData.ERROR_PREFIX
import no.nav.amt.person.service.clients.pdl.PdlClientTestData.NULL_ERROR
import no.nav.amt.person.service.clients.pdl.PdlClientTestData.flereFeilRespons
import no.nav.amt.person.service.clients.pdl.PdlClientTestData.fodselsarRespons
import no.nav.amt.person.service.clients.pdl.PdlClientTestData.gyldigRespons
import no.nav.amt.person.service.clients.pdl.PdlClientTestData.minimalFeilRespons
import no.nav.amt.person.service.clients.pdl.PdlClientTestData.telefonResponse
import no.nav.amt.person.service.data.TestData
import no.nav.amt.person.service.data.TestData.postnumreInTest
import no.nav.amt.person.service.integration.IntegrationTestBase
import no.nav.amt.person.service.person.model.IdentType
import no.nav.amt.person.service.person.model.Personident
import no.nav.amt.person.service.poststed.PoststedRepository
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
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
			oppdatertePostnummer = postnumreInTest,
			sporingsId = UUID.randomUUID(),
		)
	}

	@AfterEach
	fun tearDownLocal() = server.shutdown()

	@Nested
	inner class HentPerson {
		@Test
		fun `hentPerson - gyldig respons - skal lage riktig request og parse pdl person`() {
			// Arrange
			val connector =
				PdlClient(
					baseUrl = serverUrl,
					tokenProvider = { "TOKEN" },
					poststedRepository = poststedRepository,
					objectMapper = objectMapper,
				)

			server.enqueue(MockResponse().setBody(gyldigRespons))

			// Act
			val pdlPerson = connector.hentPerson("FNR")

			// Assert
			pdlPerson.erFalskIdentitet shouldBe true
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
			request.method shouldBe HttpMethod.POST.name()
			request.getHeader(HttpHeaders.AUTHORIZATION) shouldBe "Bearer TOKEN"
			request.getHeader(TEMA_HEADER) shouldBe GEN_TEMA_HEADER_VALUE

			val expectedJson =
				"""
				{
					"query": "${
					PdlQueries.HentPerson.query
						.replace("\n", "\\n")
						.replace("\t", "\\t")
				}",
					"variables": { "ident": "FNR" }
				}
				""".trimIndent()

			val body = request.body.readUtf8()
			body shouldEqualJson expectedJson
		}

		@Test
		fun `hentPerson - data mangler - skal kaste exception`() {
			// Arrange
			val client =
				PdlClient(
					baseUrl = serverUrl,
					tokenProvider = { "TOKEN" },
					poststedRepository = poststedRepository,
					objectMapper = objectMapper,
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

			// Act & Assert
			val exception =
				assertThrows<RuntimeException> {
					client.hentPerson("FNR")
				}

			exception.message shouldBe "$ERROR_PREFIX$NULL_ERROR- Noe gikk galt (code: null details: null)\n"

			val request = server.takeRequest()

			request.path shouldBe "/graphql"
			request.method shouldBe HttpMethod.POST.name()
		}

		@Test
		fun `hentPerson - Detaljert respons - skal kaste exception med noe detaljert informasjon`() {
			// Arrange
			val client =
				PdlClient(
					baseUrl = serverUrl,
					tokenProvider = { "TOKEN" },
					poststedRepository = poststedRepository,
					objectMapper = objectMapper,
				)

			server.enqueue(MockResponse().setBody(minimalFeilRespons))

			// Act & Assert
			val exception =
				assertThrows<RuntimeException> {
					client.hentPerson("FNR")
				}

			exception.message shouldBe ERROR_PREFIX + NULL_ERROR +
				"- Ikke tilgang til å se person (code: unauthorized details: PdlErrorDetails(type=abac-deny, cause=cause-0001-manglerrolle, policy=adressebeskyttelse_strengt_fortrolig_adresse))\n"
		}

		@Test
		fun `hentPerson - Flere feil i respons - skal kaste exception med noe detaljert informasjon`() {
			// Arrange
			val client =
				PdlClient(
					baseUrl = serverUrl,
					tokenProvider = { "TOKEN" },
					poststedRepository = poststedRepository,
					objectMapper = objectMapper,
				)

			server.enqueue(MockResponse().setBody(flereFeilRespons))

			// Act & Assert
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
	}

	@Nested
	inner class HentIdenter {
		@Test
		fun `hentIdenter skal lage riktig request og parse response`() {
			// Arrange
			val client =
				PdlClient(
					baseUrl = serverUrl,
					tokenProvider = { "TOKEN" },
					poststedRepository = poststedRepository,
					objectMapper = objectMapper,
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

			// Act
			val identer = client.hentIdenter(personident2.ident)

			// Assert
			identer shouldBe listOf(personident1, personident2)

			val request = server.takeRequest()

			request.path shouldBe "/graphql"
			request.method shouldBe HttpMethod.POST.name()
			request.getHeader(HttpHeaders.AUTHORIZATION) shouldBe "Bearer TOKEN"
			request.getHeader(TEMA_HEADER) shouldBe GEN_TEMA_HEADER_VALUE

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
	}

	@Test
	fun `hentTelefon - person har telefon - returnerer telefon`() {
		// Arrange
		val client =
			PdlClient(
				baseUrl = serverUrl,
				tokenProvider = { "TOKEN" },
				poststedRepository = poststedRepository,
				objectMapper = objectMapper,
			)

		server.enqueue(MockResponse().setBody(telefonResponse))

		// Act
		val telefon = client.hentTelefon("FNR")

		// Assert
		telefon shouldBe "+4712345678"
	}

	@Nested
	inner class HentPersonFodselsar {
		@Test
		fun `hentPersonFodselsar - person har fodselsar - returnerer fodselsar`() {
			// Arrange
			val client =
				PdlClient(
					serverUrl,
					{ "TOKEN" },
					poststedRepository = poststedRepository,
					objectMapper = objectMapper,
				)

			server.enqueue(MockResponse().setBody(fodselsarRespons))

			// Act
			val fodselsar = client.hentPersonFodselsar("FNR")

			// Assert
			fodselsar shouldBe 1976
		}

		@Test
		fun `hentPersonFodselsar - data mangler - skal kaste exception`() {
			// Arrange
			val client =
				PdlClient(
					serverUrl,
					{ "TOKEN" },
					poststedRepository = poststedRepository,
					objectMapper = objectMapper,
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

			// Act & Assert
			val exception =
				assertThrows<RuntimeException> {
					client.hentPersonFodselsar("FNR")
				}

			exception.message shouldBe "$ERROR_PREFIX$NULL_ERROR- Noe gikk galt (code: null details: null)\n"

			val request = server.takeRequest()

			request.path shouldBe "/graphql"
			request.method shouldBe HttpMethod.POST.name()
		}
	}
}
