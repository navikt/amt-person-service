package no.nav.amt.person.service.integration.mock.servers

import no.nav.amt.person.service.clients.veilarboppfolging.VeilarboppfolgingClient
import no.nav.amt.person.service.nav_bruker.Oppfolgingsperiode
import no.nav.amt.person.service.utils.JsonUtils.toJsonString
import no.nav.amt.person.service.utils.MockHttpServer
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import java.time.ZoneId

class MockVeilarboppfolgingHttpServer : MockHttpServer(name = "MockVeilarboppfolgingHttpServer") {

	fun mockHentVeilederIdent(fnr: String, veilederIdent: String?) {
		val url = "/veilarboppfolging/api/v3/hent-veileder"
		val predicate = { req: RecordedRequest ->
			val body = req.body.readUtf8()

			req.path == url
				&& req.method == "POST"
				&& body.contains(fnr)
		}

		val response = if (veilederIdent == null) {
			MockResponse().setResponseCode(204)
		} else {
			MockResponse()
				.setResponseCode(200)
				.setBody("""{"veilederIdent": "$veilederIdent"}""")
		}
		addResponseHandler(predicate, response)
	}

	fun mockHentOppfolgingperioder(fnr: String, oppfolgingsperioder: List<Oppfolgingsperiode>) {
		val url = "/veilarboppfolging/api/v3/oppfolging/hent-status"
		val predicate = { req: RecordedRequest ->
			val body = req.body.readUtf8()

			req.path == url
				&& req.method == "POST"
				&& body.contains(fnr)
		}

		val oppfolgingsstatusRespons = VeilarboppfolgingClient.OppfolgingStatus(
			oppfolgingsPerioder = oppfolgingsperioder.map {
				VeilarboppfolgingClient.OppfolgingPeriodeDTO(
					uuid = it.id,
					startDato = it.startdato.atZone(ZoneId.systemDefault()),
					sluttDato = it.sluttdato?.atZone(ZoneId.systemDefault())
				)
			}
		)

		val response = MockResponse()
				.setResponseCode(200)
				.setBody(toJsonString(oppfolgingsstatusRespons))
		addResponseHandler(predicate, response)
	}
}
