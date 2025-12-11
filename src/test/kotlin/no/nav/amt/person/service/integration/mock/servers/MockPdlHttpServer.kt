package no.nav.amt.person.service.integration.mock.servers

import no.nav.amt.person.service.clients.pdl.PdlPerson
import no.nav.amt.person.service.clients.pdl.PdlQueries
import no.nav.amt.person.service.data.TestData
import no.nav.amt.person.service.person.dbo.PersonDbo
import no.nav.amt.person.service.person.model.AdressebeskyttelseGradering
import no.nav.amt.person.service.person.model.IdentType
import no.nav.amt.person.service.person.model.Personident
import no.nav.amt.person.service.utils.GraphqlUtils
import no.nav.amt.person.service.utils.JsonUtils.staticObjectMapper
import no.nav.amt.person.service.utils.MockHttpServer
import no.nav.amt.person.service.utils.getBodyAsString
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest

class MockPdlHttpServer : MockHttpServer(name = "PdlHttpServer") {
	fun mockHentPerson(person: PersonDbo) = mockHentPerson(person.personident, TestData.lagPdlPerson(person))

	fun mockHentPerson(
		brukerFnr: String,
		mockPdlPerson: PdlPerson,
	) {
		val request =
			staticObjectMapper.writeValueAsString(
				GraphqlUtils.GraphqlQuery(
					PdlQueries.HentPerson.query,
					PdlQueries.Variables(brukerFnr),
				),
			)

		val requestPredicate = { req: RecordedRequest ->
			req.path == "/graphql" &&
				req.method == "POST" &&
				req.getBodyAsString() == request
		}

		addResponseHandler(requestPredicate, createPdlBrukerResponse(brukerFnr, mockPdlPerson))
	}

	fun mockHentIdenter(
		ident: String,
		personident: String,
	) {
		val request =
			staticObjectMapper.writeValueAsString(
				GraphqlUtils.GraphqlQuery(
					PdlQueries.HentIdenter.query,
					PdlQueries.Variables(ident),
				),
			)

		val requestPredicate = { req: RecordedRequest ->
			req.path == "/graphql" &&
				req.method == "POST" &&
				req.getBodyAsString() == request
		}

		addResponseHandler(
			requestPredicate,
			createHentIdenterResponse(Personident(personident, false, IdentType.FOLKEREGISTERIDENT)),
		)
	}

	fun mockHentTelefon(
		ident: String,
		telefon: String?,
	) {
		val request =
			staticObjectMapper.writeValueAsString(
				GraphqlUtils.GraphqlQuery(
					PdlQueries.HentTelefon.query,
					PdlQueries.Variables(ident),
				),
			)

		val requestPredicate = { req: RecordedRequest ->
			req.path == "/graphql" &&
				req.method == "POST" &&
				req.getBodyAsString() == request
		}

		addResponseHandler(requestPredicate, createHentTelefonResponse(telefon))
	}

	fun mockHentAdressebeskyttelse(
		ident: String,
		gradering: AdressebeskyttelseGradering?,
	) {
		val request =
			staticObjectMapper.writeValueAsString(
				GraphqlUtils.GraphqlQuery(
					PdlQueries.HentAdressebeskyttelse.query,
					PdlQueries.Variables(ident),
				),
			)

		val requestPredicate = { req: RecordedRequest ->
			req.path == "/graphql" &&
				req.method == "POST" &&
				req.getBodyAsString() == request
		}

		addResponseHandler(requestPredicate, createHentAdressebeskyttelseResponse(gradering))
	}

	private fun createHentAdressebeskyttelseResponse(gradering: AdressebeskyttelseGradering?): MockResponse {
		val body =
			staticObjectMapper.writeValueAsString(
				PdlQueries.HentAdressebeskyttelse.Response(
					errors = null,
					data =
						PdlQueries.HentAdressebeskyttelse.ResponseData(
							PdlQueries.HentAdressebeskyttelse.HentPerson(
								adressebeskyttelse =
									if (gradering != null) {
										listOf(PdlQueries.Attribute.Adressebeskyttelse(gradering = gradering.toString()))
									} else {
										emptyList()
									},
							),
						),
					extensions = null,
				),
			)

		return MockResponse().setResponseCode(200).setBody(body)
	}

	private fun createHentTelefonResponse(telefon: String?): MockResponse {
		val telefonnummer = telefon?.let { listOf(PdlQueries.Attribute.Telefonnummer("47", it, 1)) } ?: emptyList()

		val body =
			staticObjectMapper.writeValueAsString(
				PdlQueries.HentTelefon.Response(
					errors = null,
					data =
						PdlQueries.HentTelefon.ResponseData(
							PdlQueries.HentTelefon.HentPerson(
								telefonnummer = telefonnummer,
							),
						),
					extensions = null,
				),
			)

		return MockResponse().setResponseCode(200).setBody(body)
	}

	private fun createHentIdenterResponse(ident: Personident): MockResponse {
		val body =
			staticObjectMapper.writeValueAsString(
				PdlQueries.HentIdenter.Response(
					errors = null,
					data =
						PdlQueries.HentIdenter.ResponseData(
							PdlQueries.HentIdenter.HentIdenter(
								identer =
									listOf(
										PdlQueries.Attribute.Ident(
											ident.ident,
											ident.historisk,
											ident.type.name,
										),
									),
							),
						),
					extensions = null,
				),
			)

		return MockResponse().setResponseCode(200).setBody(body)
	}

	private fun createPdlBrukerResponse(
		personident: String,
		mockPdlPerson: PdlPerson,
	): MockResponse {
		val body =
			staticObjectMapper.writeValueAsString(
				PdlQueries.HentPerson.Response(
					errors = null,
					data =
						PdlQueries.HentPerson.ResponseData(
							PdlQueries.HentPerson.HentPerson(
								navn =
									listOf(
										PdlQueries.Attribute.Navn(
											mockPdlPerson.fornavn,
											mockPdlPerson.mellomnavn,
											mockPdlPerson.etternavn,
										),
									),
								telefonnummer = listOf(PdlQueries.Attribute.Telefonnummer("47", "12345678", 1)),
								adressebeskyttelse =
									if (mockPdlPerson.adressebeskyttelseGradering != null) {
										listOf(PdlQueries.Attribute.Adressebeskyttelse(gradering = mockPdlPerson.adressebeskyttelseGradering.toString()))
									} else {
										emptyList()
									},
								bostedsadresse =
									listOf(
										PdlQueries.Attribute.Bostedsadresse(
											coAdressenavn = "C/O Mamma",
											vegadresse =
												PdlQueries.Attribute.Vegadresse(
													husnummer = "7",
													husbokstav = null,
													adressenavn = "Gateveien",
													tilleggsnavn = "Gården",
													postnummer = "0484",
												),
											matrikkeladresse = null,
										),
									),
								oppholdsadresse = emptyList(),
								kontaktadresse =
									listOf(
										PdlQueries.Attribute.Kontaktadresse(
											coAdressenavn = null,
											vegadresse = null,
											postboksadresse =
												PdlQueries.Attribute.Postboksadresse(
													postboks = "Postboks 1234",
													postnummer = "0484",
												),
										),
									),
							),
							PdlQueries.HentPerson.HentIdenter(
								listOf(
									PdlQueries.Attribute.Ident(
										personident,
										false,
										"FOLKEREGISTERIDENT",
									),
								),
							),
						),
					extensions = null,
				),
			)

		return MockResponse()
			.setResponseCode(200)
			.setBody(body)
	}
}
