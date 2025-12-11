package no.nav.amt.person.service.clients.nom

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.amt.person.service.data.TestData
import no.nav.amt.person.service.navansatt.NavAnsattDbo
import no.nav.amt.person.service.utils.JsonUtils.staticObjectMapper
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class NomClientTest {
	private lateinit var client: NomClientImpl
	private lateinit var server: MockWebServer
	private val token = "TOKEN"

	@BeforeEach
	fun setup() {
		server = MockWebServer()
		client =
			NomClientImpl(
				url = server.url("").toString().removeSuffix("/"),
				tokenSupplier = { token },
				objectMapper = staticObjectMapper,
			)
	}

	@AfterEach
	fun cleanup() = server.shutdown()

	@Test
	fun `hentVeileder - veileder finnes ikke - returnerer null`() {
		val veilederRespons = hentRessurserResponse(emptyList(), 1)
		server.enqueue(MockResponse().setBody(veilederRespons))

		val veileder = client.hentNavAnsatt("test")
		veileder shouldBe null
	}

	@Test
	fun `hentVeiledere - veiledere finnes - returnerer veiledere`() {
		val veiledere = listOf(TestData.lagNavAnsatt(), TestData.lagNavAnsatt())
		val veilederRespons = hentRessurserResponse(veiledere)

		server.enqueue(MockResponse().setBody(veilederRespons))

		val faktiskeVeiledere = client.hentNavAnsatte(veiledere.map { it.navIdent })
		val firstFaktiskVeileder = faktiskeVeiledere.first { it.navIdent == veiledere[0].navIdent }
		val secondFaktiskVeileder = faktiskeVeiledere.first { it.navIdent == veiledere[1].navIdent }

		assertSoftly(firstFaktiskVeileder) {
			navIdent shouldBe veiledere[0].navIdent
			navn shouldBe veiledere[0].navn
			telefonnummer shouldBe veiledere[0].telefon
			epost shouldBe veiledere[0].epost
		}

		assertSoftly(secondFaktiskVeileder) {
			navIdent shouldBe veiledere[1].navIdent
			navn shouldBe veiledere[1].navn
			telefonnummer shouldBe veiledere[1].telefon
			epost shouldBe veiledere[1].epost
		}
	}

	@Test
	fun `hentVeiledere - en veileder finnes ikke - returnerer veileder som finnes`() {
		val veileder = TestData.lagNavAnsatt()
		val feilIdent = "Feil Ident"

		val veilederRespons = hentRessurserResponse(listOf(veileder), 1)

		server.enqueue(MockResponse().setBody(veilederRespons))

		val faktiskeVeiledere = client.hentNavAnsatte(listOf(veileder.navIdent, feilIdent))

		val faktiskVeileder = faktiskeVeiledere.first { it.navIdent == veileder.navIdent }

		faktiskeVeiledere.find { it.navIdent == feilIdent } shouldBe null
		faktiskeVeiledere shouldHaveSize 1

		assertSoftly(faktiskVeileder) {
			navIdent shouldBe veileder.navIdent
			navn shouldBe veileder.navn
			telefonnummer shouldBe veileder.telefon
			epost shouldBe veileder.epost
		}
	}

	private fun hentRessurserResponse(
		veiledere: List<NavAnsattDbo>,
		antallNotFount: Int = 0,
	): String {
		val ressurser =
			veiledere.map {
				"""
				{
					"ressurs": {
						"navident": "${it.navIdent}",
						"visningsnavn": "${it.navn}",
						"fornavn": "Fornavn",
						"etternavn": "Etternavn",
						"epost": "${it.epost}",
						"telefon": [{ "type": "NAV_TJENESTE_TELEFON", "nummer": "${it.telefon}" }],
						"orgTilknytning": [
							{
							  "gyldigTom": null,
							  "orgEnhet": {
								"remedyEnhetId": "0315"
							  },
							  "erDagligOppfolging": true,
							  "gyldigFom": "2015-01-01"
							}
						]
					},
					"code": "OK"
				}
				""".trimIndent()
			}

		val notFound =
			(0..antallNotFount).map {
				"""
				{
					"code": "NOT_FOUND",
					"ressurs": null
				}
			"""
			}

		val veilederRespons =
			"""
			{
			  "data": {
				"ressurser": [ ${ressurser.plus(notFound).joinToString { it }} ]
			  }
			}
			""".trimIndent()

		return veilederRespons
	}
}
