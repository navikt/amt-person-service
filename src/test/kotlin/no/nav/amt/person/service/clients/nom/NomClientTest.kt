package no.nav.amt.person.service.clients.nom

import io.kotest.assertions.fail
import io.kotest.matchers.shouldBe
import no.nav.amt.person.service.data.TestData
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class NomClientTest {
	private lateinit var client: NomClient
	private lateinit var server: MockWebServer
	private val token = "TOKEN"

	@BeforeEach
	fun setup() {
		server = MockWebServer()
		client = NomClient(
			url = server.url("").toString().removeSuffix("/"),
			tokenSupplier = { token },
		)
	}

	@AfterEach
	fun cleanup() {
		server.shutdown()
	}

	@Test
	fun `hentVeileder - veileder finnes - returnerer veileder med alias`() {
		val veilederRespons = """
			{
			  "data": {
				"ressurser": [
				  {
					"ressurs": {
					  "navident": "H156147",
					  "visningsnavn": "Alias",
					  "fornavn": "Blaut",
					  "etternavn": "Slappfisk",
					  "epost": "blaut.slappfisk@nav.no",
					  "telefon": [{ "type": "NAV_TJENESTE_TELEFON", "nummer": "12345678" }]
					},
					"code": "OK"
				  }
				]
			  }
			}
		""".trimIndent()

		server.enqueue(MockResponse().setBody(veilederRespons))

		val veileder = client.hentNavAnsatt("H156147") ?: fail("Veileder er null")

		veileder.navIdent shouldBe "H156147"
		veileder.navn shouldBe "Alias"
		veileder.epost shouldBe "blaut.slappfisk@nav.no"
		veileder.telefonnummer shouldBe "12345678"
	}

	@Test
	fun `hentVeileder - veileder finnes - returnerer veileder uten alias`() {
		val veilederRespons = """
			{
			  "data": {
				"ressurser": [
				  {
					"ressurs": {
					  "navident": "H156147",
					  "visningsnavn": null,
					  "fornavn": "Blaut",
					  "etternavn": "Slappfisk",
					  "epost": "blaut.slappfisk@nav.no",
					  "telefon": [{ "type": "NAV_TJENESTE_TELEFON", "nummer": "12345678" }]
					},
					"code": "OK"
				  }
				]
			  }
			}
		""".trimIndent()

		server.enqueue(MockResponse().setBody(veilederRespons))

		val veileder = client.hentNavAnsatt("H156147") ?: fail("Veileder er null")

		veileder.navIdent shouldBe "H156147"
		veileder.navn shouldBe "Blaut Slappfisk"
		veileder.epost shouldBe "blaut.slappfisk@nav.no"
		veileder.telefonnummer shouldBe "12345678"
	}

	@Test
	fun `hentVeileder - skal ikke hente privat telefonnummer`() {
		val veilederRespons = """
			{
			  "data": {
				"ressurser": [
				  {
					"ressurs": {
					  "navident": "H156147",
					  "visningsnavn": "Alias",
					  "fornavn": "Blaut",
					  "etternavn": "Slappfisk",
					  "epost": "blaut.slappfisk@nav.no",
					  "telefon": [{ "type": "PRIVAT_TELEFON", "nummer": "12345678" }]
					},
					"code": "OK"
				  }
				]
			  }
		}
		""".trimIndent()

		server.enqueue(MockResponse().setBody(veilederRespons))

		val veileder = client.hentNavAnsatt("H156147") ?: fail("Veileder er null")

		veileder.telefonnummer shouldBe null
	}

	@Test
	fun `hentVeileder - veileder finnes ikke - returnerer null`() {
		val veilederRespons = """
			{
				"data": {
					"ressurser": [
					  {
						"code": "NOT_FOUND",
						"ressurs": null
					  }
					]
				}
			}
		""".trimIndent()

		server.enqueue(MockResponse().setBody(veilederRespons))

		val veileder = client.hentNavAnsatt("test")
		veileder shouldBe null
	}

	@Test
	fun `hentVeileder - token legges på - token mottas på serverfun `() {
		val veilederRespons = """
			{
				"data": {
					"ressurser": [
					  {
						"code": "NOT_FOUND",
						"ressurs": null
					  }
					]
				}
			}
		""".trimIndent()

		server.enqueue(MockResponse().setBody(veilederRespons))

		client.hentNavAnsatt("test")

		val recordedRequest = server.takeRequest()

		recordedRequest.getHeader("Authorization") shouldBe "Bearer $token"
	}


	@Test
	fun `hentVeileder - skal ikke hente kontortelefon først`() {
		val kontorTelefon = "11111111"
		val tjenesteTelefon = "22222222"
		val veilederRespons = """
			{
			  "data": {
				"ressurser": [
				  {
					"ressurs": {
					  "navident": "H156147",
					  "visningsnavn": "Alias",
					  "fornavn": "Blaut",
					  "etternavn": "Slappfisk",
					  "epost": "blaut.slappfisk@nav.no",
					  "telefon": [
					  	{ "type": "NAV_TJENESTE_TELEFON", "nummer": "$tjenesteTelefon" },
					  	{ "type": "NAV_KONTOR_TELEFON", "nummer": "$kontorTelefon" }
					  ]
					},
					"code": "OK"
				  }
				]
			  }
		}
		""".trimIndent()

		server.enqueue(MockResponse().setBody(veilederRespons))

		val veileder = client.hentNavAnsatt("H156147") ?: fail("Veileder finnes ikke")

		veileder.telefonnummer shouldBe kontorTelefon
	}

	@Test
	fun `hentVeileder - skal ikke hente tjenestetelefon naar kontortelefon mangler`() {
		val tjenesteTelefon = "22222222"
		val veilederRespons = """
			{
			  "data": {
				"ressurser": [
				  {
					"ressurs": {
					  "navident": "H156147",
					  "visningsnavn": "Alias",
					  "fornavn": "Blaut",
					  "etternavn": "Slappfisk",
					  "epost": "blaut.slappfisk@nav.no",
					  "telefon": [
					  	{ "type": "NAV_TJENESTE_TELEFON", "nummer": "$tjenesteTelefon" }
					  ]
					},
					"code": "OK"
				  }
				]
			  }
		}
		""".trimIndent()

		server.enqueue(MockResponse().setBody(veilederRespons))

		val veileder = client.hentNavAnsatt("H156147") ?: fail("Fant ikke veileder")

		veileder.telefonnummer shouldBe tjenesteTelefon
	}

	@Test
	fun `hentVeiledere - veiledere finnes - returnerer veiledere`() {
		val veiledere = listOf(TestData.lagNavAnsatt(), TestData.lagNavAnsatt())
		val veilederRespons = """
			{
			  "data": {
				"ressurser": [
				  {
					"ressurs": {
					  "navident": "${veiledere[0].navIdent}",
					  "visningsnavn": "${veiledere[0].navn}",
					  "fornavn": "Fornavn",
					  "etternavn": "Etternavn",
					  "epost": "${veiledere[0].epost}",
					  "telefon": [{ "type": "NAV_TJENESTE_TELEFON", "nummer": "${veiledere[0].telefon}" }]
					},
					"code": "OK"
				  },
				  {
					"ressurs": {
					  "navident": "${veiledere[1].navIdent}",
					  "visningsnavn": "${veiledere[1].navn}",
					  "fornavn": "Fornavn",
					  "etternavn": "Etternavn",
					  "epost": "${veiledere[1].epost}",
					  "telefon": [{ "type": "NAV_TJENESTE_TELEFON", "nummer": "${veiledere[1].telefon}" }]
					},
					"code": "OK"
				  }
				]
			  }
			}
		""".trimIndent()

		server.enqueue(MockResponse().setBody(veilederRespons))

		val faktiskeVeiledere = client.hentNavAnsatte(veiledere.map { it.navIdent })
		val v1 = faktiskeVeiledere.find { it.navIdent == veiledere[0].navIdent }!!
		val v2 = faktiskeVeiledere.find { it.navIdent == veiledere[1].navIdent }!!

		v1.navIdent shouldBe veiledere[0].navIdent
		v1.navn shouldBe veiledere[0].navn
		v1.telefonnummer shouldBe veiledere[0].telefon
		v1.epost shouldBe veiledere[0].epost

		v2.navIdent shouldBe veiledere[1].navIdent
		v2.navn shouldBe veiledere[1].navn
		v2.telefonnummer shouldBe veiledere[1].telefon
		v2.epost shouldBe veiledere[1].epost
	}
}
