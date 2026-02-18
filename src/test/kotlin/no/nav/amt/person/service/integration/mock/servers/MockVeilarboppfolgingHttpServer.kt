package no.nav.amt.person.service.integration.mock.servers

import no.nav.amt.person.service.clients.veilarboppfolging.VeilarboppfolgingClient
import no.nav.amt.person.service.navbruker.Oppfolgingsperiode
import no.nav.amt.person.service.utils.JsonUtils.staticObjectMapper
import no.nav.amt.person.service.utils.MockHttpServer
import no.nav.amt.person.service.utils.getBodyAsString
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.springframework.http.HttpStatus
import java.time.ZoneId

class MockVeilarboppfolgingHttpServer : MockHttpServer(name = "MockVeilarboppfolgingHttpServer") {
	fun mockHentVeilederIdent(
		fnr: String,
		veilederIdent: String?,
	) {
		val url = "/veilarboppfolging/api/v3/hent-veileder"
		val predicate = { req: RecordedRequest ->
			val body = req.getBodyAsString()

			req.path == url &&
				req.method == "POST" &&
				body.contains(fnr)
		}

		val response =
			if (veilederIdent == null) {
				MockResponse().setResponseCode(HttpStatus.NO_CONTENT.value())
			} else {
				MockResponse()
					.setResponseCode(HttpStatus.OK.value())
					.setBody("""{"veilederIdent": "$veilederIdent"}""")
			}
		addResponseHandler(predicate, response)
	}

	fun mockHentOppfolgingperioder(
		fnr: String,
		oppfolgingsperioder: List<Oppfolgingsperiode>,
	) {
		val url = "/veilarboppfolging/api/v3/oppfolging/hent-perioder"
		val predicate = { req: RecordedRequest ->
			val body = req.getBodyAsString()

			req.path == url &&
				req.method == "POST" &&
				body.contains(fnr)
		}

		val oppfolgingsperioderRespons =
			oppfolgingsperioder.map {
				VeilarboppfolgingClient.OppfolgingPeriodeDTO(
					uuid = it.id,
					startDato = it.startdato.atZone(ZoneId.systemDefault()),
					sluttDato = it.sluttdato?.atZone(ZoneId.systemDefault()),
				)
			}

		val response =
			MockResponse()
				.setResponseCode(HttpStatus.OK.value())
				.setBody(staticObjectMapper.writeValueAsString(oppfolgingsperioderRespons))
		addResponseHandler(predicate, response)
	}
}
