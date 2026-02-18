package no.nav.amt.person.service.integration.mock.servers

import no.nav.amt.person.service.clients.veilarbvedtaksstotte.VeilarbvedtaksstotteClient
import no.nav.amt.person.service.navbruker.InnsatsgruppeV2
import no.nav.amt.person.service.utils.JsonUtils.staticObjectMapper
import no.nav.amt.person.service.utils.MockHttpServer
import no.nav.amt.person.service.utils.getBodyAsString
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus

class MockVeilarbvedtaksstotteHttpServer : MockHttpServer(name = "MockVeilarbvedtaksstotteHttpServer") {
	fun mockHentInnsatsgruppe(
		fnr: String,
		innsatsgruppe: InnsatsgruppeV2?,
	) {
		val url = "/veilarbvedtaksstotte/api/hent-gjeldende-14a-vedtak"
		val predicate = { req: RecordedRequest ->
			val body = req.getBodyAsString()

			req.path == url &&
				req.method == HttpMethod.POST.name() &&
				body.contains(fnr)
		}

		val body =
			innsatsgruppe?.let {
				staticObjectMapper.writeValueAsString(
					VeilarbvedtaksstotteClient.Gjeldende14aVedtakDTO(
						innsatsgruppe = it,
					),
				)
			}

		val response =
			body?.let {
				MockResponse().setResponseCode(HttpStatus.OK.value()).setBody(it)
			} ?: MockResponse().setResponseCode(HttpStatus.NO_CONTENT.value())

		addResponseHandler(predicate, response)
	}
}
