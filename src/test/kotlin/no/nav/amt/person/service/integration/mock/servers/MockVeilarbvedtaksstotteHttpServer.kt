package no.nav.amt.person.service.integration.mock.servers

import no.nav.amt.person.service.clients.veilarbvedtaksstotte.VeilarbvedtaksstotteClient
import no.nav.amt.person.service.navbruker.Innsatsgruppe
import no.nav.amt.person.service.utils.JsonUtils.toJsonString
import no.nav.amt.person.service.utils.MockHttpServer
import no.nav.amt.person.service.utils.getBodyAsString
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest

class MockVeilarbvedtaksstotteHttpServer : MockHttpServer(name = "MockVeilarbvedtaksstotteHttpServer") {
	fun mockHentInnsatsgruppe(
		fnr: String,
		innsatsgruppe: Innsatsgruppe?,
	) {
		val url = "/veilarbvedtaksstotte/api/hent-gjeldende-14a-vedtak"
		val predicate = { req: RecordedRequest ->
			val body = req.getBodyAsString()

			req.path == url &&
				req.method == "POST" &&
				body.contains(fnr)
		}

		val body =
			innsatsgruppe?.let { toJsonString(VeilarbvedtaksstotteClient.Gjeldende14aVedtakDTO(innsatsgruppe = it)) }
		val response =
			body?.let {
				MockResponse()
					.setResponseCode(200)
					.setBody(it)
			} ?: MockResponse()
				.setResponseCode(200)

		addResponseHandler(predicate, response)
	}
}
