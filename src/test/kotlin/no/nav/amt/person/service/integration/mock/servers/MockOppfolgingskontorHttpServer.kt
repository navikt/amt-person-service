package no.nav.amt.person.service.integration.mock.servers
import no.nav.amt.person.service.utils.MockHttpServer
import no.nav.amt.person.service.utils.getBodyAsString
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus

class MockOppfolgingskontorHttpServer : MockHttpServer(name = "MockOppfolgingskontorHttpServer") {
    fun mockHentKontorForBruker(
        ident: String,
        kontorId: String?,
    ) {
        val predicate = { req: RecordedRequest ->
            req.path == "/graphql" &&
                req.method == HttpMethod.POST.name() &&
                req.getBodyAsString().contains(ident)
        }
        val kontorJson =
            if (kontorId == null) {
                """{"arbeidsoppfolging": null}"""
            } else {
                """{"arbeidsoppfolging": {"kontorId": "$kontorId", "kontorNavn": "NAV Testkontor"}}"""
            }
        val response =
            MockResponse()
                .setResponseCode(HttpStatus.OK.value())
                .setBody("""{"data": {"kontorTilhorigheter": $kontorJson}}""")
        addResponseHandler(predicate, response)
    }
}
