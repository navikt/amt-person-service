package no.nav.amt.person.service.integration.mock.servers

import no.nav.amt.person.service.clients.pdl.PdlQueries
import no.nav.amt.person.service.person.model.AdressebeskyttelseGradering
import no.nav.amt.person.service.person.model.Person
import no.nav.amt.person.service.utils.GraphqlUtils
import no.nav.amt.person.service.utils.JsonUtils.toJsonString
import no.nav.amt.person.service.utils.MockHttpServer
import no.nav.amt.person.service.utils.getBodyAsString
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest

class MockPdlHttpServer : MockHttpServer(name = "PdlHttpServer") {

	fun mockHentPerson(person: Person) {
		mockHentPerson(person.personIdent, MockPdlBruker(
			fornavn = person.fornavn,
			etternavn = person.etternavn,
			adressebeskyttelse = null,
		))
	}
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
				&& req.getBodyAsString() == request
		}

		addResponseHandler(requestPredicate, createPdlBrukerResponse(brukerFnr, mockPdlBruker))
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

	fun mockHentAdressebeskyttelse(ident: String, gradering: AdressebeskyttelseGradering) {
		val request = toJsonString(
			GraphqlUtils.GraphqlQuery(
				PdlQueries.HentAdressebeskyttelse.query,
				PdlQueries.HentAdressebeskyttelse.Variables(ident)
			)
		)

		val requestPredicate = { req: RecordedRequest ->
			req.path == "/graphql"
				&& req.method == "POST"
				&& req.getBodyAsString() == request
		}

		addResponseHandler(requestPredicate, createPdlAdressebeskyttelseResponse(gradering))
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

	private fun createPdlBrukerResponse(personIdent: String, mockPdlBruker: MockPdlBruker): MockResponse {
		val body = toJsonString(
			PdlQueries.HentPerson.Response(
				errors = null,
				data = PdlQueries.HentPerson.ResponseData(
					PdlQueries.HentPerson.HentPerson(
						navn = listOf(PdlQueries.HentPerson.Navn(mockPdlBruker.fornavn, null, mockPdlBruker.etternavn)),
						telefonnummer = listOf(PdlQueries.HentPerson.Telefonnummer("47", "12345678", 1)),
						adressebeskyttelse = if (mockPdlBruker.adressebeskyttelse != null) {
							listOf(PdlQueries.Adressebeskyttelse(mockPdlBruker.adressebeskyttelse.name))
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

	private fun createPdlAdressebeskyttelseResponse(gradering: AdressebeskyttelseGradering): MockResponse {
		val body = toJsonString(
			PdlQueries.HentAdressebeskyttelse.Response(
				errors = null,
				data = PdlQueries.HentAdressebeskyttelse.ResponseData(
					PdlQueries.HentAdressebeskyttelse.HentAdressebeskyttelse(
						listOf(PdlQueries.Adressebeskyttelse(gradering.name))
					)
				),
				extensions = null,
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
