package no.nav.amt.person.service.integration.mock.servers

import no.nav.amt.person.service.clients.nom.NomQueries
import no.nav.amt.person.service.navansatt.NavAnsattDbo
import no.nav.amt.person.service.utils.JsonUtils.staticObjectMapper
import no.nav.amt.person.service.utils.MockHttpServer
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import tools.jackson.databind.JsonNode
import tools.jackson.module.kotlin.readValue
import java.time.LocalDate

class MockNomHttpServer : MockHttpServer(name = "MockNomHttpServer") {
	fun mockHentNavAnsatt(navAnsatt: NavAnsattDbo) {
		val predicate = { req: RecordedRequest ->
			req.path == "/graphql" &&
				req.method == HttpMethod.POST.name() &&
				containsIdentifier(req, navAnsatt.navIdent)
		}

		val input =
			NomClientResponseInput(
				navident = navAnsatt.navIdent,
				visningsnavn = navAnsatt.navn,
				fornavn = navAnsatt.navn.split(" ").first(),
				etternavn = navAnsatt.navn.split(" ").last(),
				epost = navAnsatt.epost,
				telefon =
					navAnsatt.telefon?.let {
						listOf(
							NomQueries.HentRessurser.Telefon(
								nummer = it,
								type = "NAV_TJENESTE_TELEFON",
							),
						)
					} ?: emptyList(),
			)

		addResponseHandler(predicate, createResponse(input))
	}

	private fun containsIdentifier(
		req: RecordedRequest,
		identifier: String,
	): Boolean {
		val body = staticObjectMapper.readValue<JsonNode>(req.body.readUtf8())
		val identer = staticObjectMapper.readValue<List<String>>(body.get("variables").get("identer").toString())
		return identer.contains(identifier)
	}

	private fun createResponse(input: NomClientResponseInput): MockResponse {
		val body =
			NomQueries.HentRessurser.Response(
				errors = emptyList(),
				data =
					NomQueries.HentRessurser.ResponseData(
						listOf(
							NomQueries.HentRessurser.RessursResult(
								code = NomQueries.HentRessurser.ResultCode.OK,
								ressurs =
									NomQueries.HentRessurser.Ressurs(
										navident = input.navident,
										visningsnavn = input.visningsnavn,
										fornavn = input.fornavn,
										etternavn = input.etternavn,
										epost = input.epost,
										telefon = input.telefon,
										orgTilknytning = input.orgTilknytning,
										primaryTelefon = null,
									),
							),
						),
					),
			)

		return MockResponse()
			.setResponseCode(HttpStatus.OK.value())
			.setBody(staticObjectMapper.writeValueAsString(body))
	}

	data class NomClientResponseInput(
		val navident: String,
		val visningsnavn: String?,
		val fornavn: String?,
		val etternavn: String?,
		val epost: String?,
		val telefon: List<NomQueries.HentRessurser.Telefon>,
		val orgTilknytning: List<NomQueries.HentRessurser.OrgTilknytning> =
			listOf(
				NomQueries.HentRessurser.OrgTilknytning(
					gyldigFom = LocalDate.of(2020, 1, 1),
					gyldigTom = null,
					orgEnhet = NomQueries.HentRessurser.OrgTilknytning.OrgEnhet("0315"),
					erDagligOppfolging = true,
				),
			),
	)
}
