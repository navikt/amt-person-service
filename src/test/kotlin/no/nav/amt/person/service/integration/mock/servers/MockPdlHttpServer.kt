package no.nav.amt.person.service.integration.mock.servers

import no.nav.amt.person.service.clients.pdl.AdressebeskyttelseGradering
import no.nav.amt.person.service.clients.pdl.PdlQueries
import no.nav.amt.person.service.utils.GraphqlUtils
import no.nav.amt.person.service.utils.JsonUtils.toJsonString
import no.nav.amt.person.service.utils.MockHttpServer
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest

class MockPdlHttpServer : MockHttpServer(name = "PdlHttpServer") {

	fun mockHentPerson(brukerFnr: String, mockPdlBruker: MockPdlBruker) {
		val request = toJsonString(
				GraphqlUtils.GraphqlQuery(
					PdlQueries.HentPerson.query,
					PdlQueries.HentPerson.Variables(brukerFnr)
				)
			)

		val requestPredicate = { req: RecordedRequest ->
			req.path == "/graphql"
				&& req.method == "POST"
				&& req.body.readUtf8() == request
		}

		addResponseHandler(requestPredicate, createPdlBrukerResponse(mockPdlBruker))
	}

	private fun createPdlBrukerResponse(mockPdlBruker: MockPdlBruker): MockResponse {
		val body = toJsonString(
			PdlQueries.HentPerson.Response(
				errors = null,
				data = PdlQueries.HentPerson.ResponseData(
					PdlQueries.HentPerson.HentPerson(
						navn = listOf(PdlQueries.HentPerson.Navn(mockPdlBruker.fornavn, null, mockPdlBruker.etternavn)),
						telefonnummer = listOf(PdlQueries.HentPerson.Telefonnummer("47", "12345678", 1)),
						adressebeskyttelse = if (mockPdlBruker.adressebeskyttelse != null) {
							listOf(PdlQueries.HentPerson.Adressebeskyttelse(mockPdlBruker.adressebeskyttelse.name))
						} else {
							emptyList()
						}
					),
					PdlQueries.HentPerson.HentIdenter(listOf(PdlQueries.HentPerson.Ident("PERSON_IDENT", false, "FOLKEREGISTERIDENT")))
				)
			)
		)

		return MockResponse()
			.setResponseCode(200)
			.setBody(body)
	}
}

data class MockPdlBruker(
	val fornavn: String = "Ola",
	val etternavn: String = "Nordmann",
	val adressebeskyttelse: AdressebeskyttelseGradering? = null,
)
