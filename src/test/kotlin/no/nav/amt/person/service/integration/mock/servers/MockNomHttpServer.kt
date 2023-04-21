package no.nav.amt.person.service.integration.mock.servers

import com.fasterxml.jackson.databind.JsonNode
import no.nav.amt.person.service.clients.nom.NomQueries
import no.nav.amt.person.service.utils.JsonUtils.fromJsonString
import no.nav.amt.person.service.utils.JsonUtils.toJsonString
import no.nav.amt.person.service.utils.MockHttpServer
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest

class MockNomHttpServer : MockHttpServer(name = "MockNomHttpServer") {

	fun addDefaultData() {
		mockHentNavAnsatt(
			NomClientResponseInput(
				navident = "NavIdent",
				visningsnavn = "Jeg er en test",
				fornavn = "INTEGRASJON",
				etternavn = "TEST",
				epost = "integrasjon.test@nav.no",
				telefon = listOf(
					NomQueries.HentIdenter.Telefon("12345678", "NAV_TJENESTE_TELEFON")
				)
			)
		)
	}

	fun mockHentNavAnsatt(input: NomClientResponseInput) {
		val predicate = { req: RecordedRequest ->
			req.path == "/graphql"
				&& req.method == "POST"
				&& containsIdentifier(req, input.navident)
		}

		addResponseHandler(predicate, createResponse(input))
	}

	private fun containsIdentifier(req: RecordedRequest, identifier: String): Boolean {
		val body = fromJsonString<JsonNode>(req.body.readUtf8())
		val identer = fromJsonString<List<String>>(body.get("variables").get("identer").toString())
		return identer.contains(identifier)
	}

	private fun createResponse(input: NomClientResponseInput): MockResponse {
		val body = NomQueries.HentIdenter.Response(
			errors = emptyList(),
			data = NomQueries.HentIdenter.ResponseData(
				listOf(
					NomQueries.HentIdenter.RessursResult(
						code = NomQueries.HentIdenter.ResultCode.OK,
						ressurs = NomQueries.HentIdenter.Ressurs(
							navident = input.navident,
							visningsnavn = input.visningsnavn,
							fornavn = input.fornavn,
							etternavn = input.etternavn,
							epost = input.epost,
							telefon = input.telefon
						)
					)
				)
			)
		)

		return MockResponse()
			.setResponseCode(200)
			.setBody(toJsonString(body))
	}

	data class NomClientResponseInput(
		val navident: String,
		val visningsnavn: String?,
		val fornavn: String?,
		val etternavn: String?,
		val epost: String?,
		val telefon: List<NomQueries.HentIdenter.Telefon>,
	)
}
