package no.nav.amt.person.service.clients.oppfolgingskontor

import no.nav.amt.person.service.clients.HeaderConstants
import no.nav.amt.person.service.clients.HeaderConstants.NAV_CONSUMER_ID_HEADER_VALUE
import no.nav.common.token_client.client.MachineToMachineTokenClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.body

@Service
class OppfolgingskontorClient(
    @Value($$"${ao-oppfolgingskontor.url}") baseUrl: String,
    @Value($$"${ao-oppfolgingskontor.scope}") private val scope: String,
    private val machineToMachineTokenClient: MachineToMachineTokenClient,
    restClientBuilder: RestClient.Builder,
) {
    companion object {
        private val kontorForBrukerQuery =
            $$"""
            query HentKontorer($ident: String!) {
              kontorTilhorigheter(ident: $ident) {
                arbeidsoppfolging {
                    kontorId   
                    kontorNavn 
                }
              }
            }
            """.trimIndent()
    }

    private val restClient: RestClient = restClientBuilder
        .baseUrl(baseUrl)
        .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        .defaultHeader(HeaderConstants.NAV_CONSUMER_ID_HEADER, NAV_CONSUMER_ID_HEADER_VALUE)
        .defaultRequest {
            it.header(HttpHeaders.AUTHORIZATION, "Bearer ${machineToMachineTokenClient.createMachineToMachineToken(scope)}")
        }.build()

    fun hentKontorForBruker(ident: String): Arbeidsoppfolging? {
        val gqlResponse = restClient
            .post()
            .uri("/graphql")
            .body(
                GraphQLRequest(
                    query = kontorForBrukerQuery,
                    variables = mapOf("ident" to ident),
                ),
            ).retrieve()
            .body<GraphQLResponse<HentKontorerResponse>>()
            ?: throw RuntimeException("Tomt svar fra ao-oppfolgingskontor")

        gqlResponse.errors?.takeIf { it.isNotEmpty() }?.let { errors ->
            val melding = errors.joinToString(separator = "\n") { "- ${it.message}" }
            throw RuntimeException("Feilmeldinger i respons fra ao-oppfolgingskontor:\n$melding")
        }

        if (gqlResponse.data == null) {
            throw RuntimeException("ao-oppfolgingskontor respons inneholder ikke data")
        }

        return gqlResponse.data.kontorTilhorigheter.arbeidsoppfolging
    }

    private data class GraphQLRequest(
        val query: String,
        val variables: Map<String, String> = emptyMap(),
    )

    private data class GraphQLResponse<T>(
        val data: T? = null,
        val errors: List<GraphQLError>? = null,
    )

    private data class GraphQLError(
        val message: String,
    )

    private data class HentKontorerResponse(
        val kontorTilhorigheter: KontorTilhorigheter,
    )

    private data class KontorTilhorigheter(
        val arbeidsoppfolging: Arbeidsoppfolging? = null,
    )
}
