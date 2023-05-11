package no.nav.amt.person.service.integration.mock.servers

import no.nav.amt.person.service.clients.pdl.PdlPerson
import no.nav.amt.person.service.clients.pdl.PdlQueries
import no.nav.amt.person.service.data.TestData
import no.nav.amt.person.service.person.dbo.PersonDbo
import no.nav.amt.person.service.utils.GraphqlUtils
import no.nav.amt.person.service.utils.JsonUtils.toJsonString
import no.nav.amt.person.service.utils.MockHttpServer
import no.nav.amt.person.service.utils.getBodyAsString
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest

class MockPdlHttpServer : MockHttpServer(name = "PdlHttpServer") {

	fun mockHentPerson(person: PersonDbo) {
		mockHentPerson(person.personIdent, TestData.lagPdlPerson(person))
	}
	fun mockHentPerson(brukerFnr: String, mockPdlPerson: PdlPerson) {
		val request = toJsonString(
				GraphqlUtils.GraphqlQuery(
					PdlQueries.HentPerson.query,
					PdlQueries.HentPerson.Variables(brukerFnr)
				)
			)

		val requestPredicate = { req: RecordedRequest ->
			req.path == "/graphql"
				&& req.method == "POST"
				&& req.getBodyAsString() == request
		}

		addResponseHandler(requestPredicate, createPdlBrukerResponse(brukerFnr, mockPdlPerson))
	}

	fun mockHentGjeldendePersonligIdent(ident: String, personIdent: String) {
		val request = toJsonString(
			GraphqlUtils.GraphqlQuery(
				PdlQueries.HentGjeldendeIdent.query,
				PdlQueries.HentGjeldendeIdent.Variables(ident)
			)
		)

		val requestPredicate = { req: RecordedRequest ->
			req.path == "/graphql"
				&& req.method == "POST"
				&& req.getBodyAsString() == request
		}

		addResponseHandler(requestPredicate, createHentGjeldendeIdentResponse(personIdent))
	}

	fun mockHentTelefon(ident: String, telefon: String?) {
		val request = toJsonString(
			GraphqlUtils.GraphqlQuery(
				PdlQueries.HentTelefon.query,
				PdlQueries.HentTelefon.Variables(ident)
			)
		)

		val requestPredicate = { req: RecordedRequest ->
			req.path == "/graphql"
				&& req.method == "POST"
				&& req.getBodyAsString() == request
		}

		addResponseHandler(requestPredicate, createHentTelefonResponse(telefon))
	}

	private fun createHentTelefonResponse(telefon: String?): MockResponse {
		val telefonnummer = telefon?.let { listOf(PdlQueries.Telefonnummer("47", it, 1)) } ?: emptyList()

		val body = toJsonString(
			PdlQueries.HentTelefon.Response(
				errors = null,
				data = PdlQueries.HentTelefon.ResponseData(
					PdlQueries.HentTelefon.HentPerson(
						telefonnummer = telefonnummer
					)
				),
				extensions = null,
			)
		)

		return MockResponse().setResponseCode(200).setBody(body)
	}


	private fun createHentGjeldendeIdentResponse(personIdent: String): MockResponse {
		val body = toJsonString(
			PdlQueries.HentGjeldendeIdent.Response(
				errors = null,
				data = PdlQueries.HentGjeldendeIdent.ResponseData(
					PdlQueries.HentGjeldendeIdent.HentIdenter(
						identer = listOf(PdlQueries.HentGjeldendeIdent.Ident(ident = personIdent))
					)
				),
				extensions = null,
			)
		)

		return MockResponse().setResponseCode(200).setBody(body)
	}

	private fun createPdlBrukerResponse(personIdent: String, mockPdlPerson: PdlPerson): MockResponse {
		val body = toJsonString(
			PdlQueries.HentPerson.Response(
				errors = null,
				data = PdlQueries.HentPerson.ResponseData(
					PdlQueries.HentPerson.HentPerson(
						navn = listOf(PdlQueries.HentPerson.Navn(mockPdlPerson.fornavn, null, mockPdlPerson.etternavn)),
						telefonnummer = listOf(PdlQueries.Telefonnummer("47", "12345678", 1)),
						adressebeskyttelse = if (mockPdlPerson.adressebeskyttelseGradering != null) {
							listOf(PdlQueries.HentPerson.Adressebeskyttelse(gradering = mockPdlPerson.adressebeskyttelseGradering.toString()))
						} else {
							emptyList()
						}
					),
					PdlQueries.HentPerson.HentIdenter(listOf(PdlQueries.HentPerson.Ident(personIdent, false, "FOLKEREGISTERIDENT")))
				),
				extensions = null,
			)
		)

		return MockResponse()
			.setResponseCode(200)
			.setBody(body)
	}

}
