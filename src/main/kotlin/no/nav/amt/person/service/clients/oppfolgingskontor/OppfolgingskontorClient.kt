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
        val response = GraphqlResponse(jsonResponse, objectMapper)

        response.errors?.let { errors ->
            val melding = errors.joinToString(separator = "\n") { "- ${it["message"]?.asString()}" }
            throw RuntimeException("Feilmeldinger i respons fra ao-oppfolgingskontor:\n$melding")
        }

        val kontorTilhorigheter: KontorTilhorigheter = response.requiredDataAt("kontorTilhorigheter")

        return kontorTilhorigheter.arbeidsoppfolging
    }

    companion object {
        private val kontorForBrukerQuery = GraphqlResponse.loadDocument("hentKontorForBruker")
    }
}

data class KontorTilhorigheter(
    val arbeidsoppfolging: Arbeidsoppfolging? = null,
)
