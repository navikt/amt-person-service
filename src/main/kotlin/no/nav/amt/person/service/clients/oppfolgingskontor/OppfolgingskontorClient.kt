package no.nav.amt.person.service.clients.oppfolgingskontor

import no.nav.amt.person.service.clients.GraphqlRequest
import no.nav.amt.person.service.clients.GraphqlResponse
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper

@Service
class OppfolgingskontorClient(
    private val api: OppfolgingskontorApi,
    private val objectMapper: ObjectMapper,
) {
    fun hentKontorForBruker(ident: String): Arbeidsoppfolging? {
        val jsonResponse = api.execute(GraphqlRequest(kontorForBrukerQuery, mapOf("ident" to ident)))
        val response = GraphqlResponse(jsonResponse, objectMapper, "ao-oppfolgingskontor")
        response.throwOnErrors()

        val kontorTilhorigheter: KontorTilhorigheter = response.requiredDataAt("kontorTilhorigheter")

        return kontorTilhorigheter.arbeidsoppfolging
    }

    companion object {
        private val kontorForBrukerQuery = GraphqlResponse.loadDocument("hentKontorForBruker")
    }

    private data class KontorTilhorigheter(
        val arbeidsoppfolging: Arbeidsoppfolging? = null,
    )
}
