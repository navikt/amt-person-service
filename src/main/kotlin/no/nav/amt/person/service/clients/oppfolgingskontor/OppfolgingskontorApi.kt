package no.nav.amt.person.service.clients.oppfolgingskontor

import no.nav.amt.person.service.clients.AO_OPPFOLGINGSKONTOR_CLIENT_ID
import org.springframework.security.oauth2.client.annotation.ClientRegistrationId
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.service.annotation.HttpExchange
import org.springframework.web.service.annotation.PostExchange

@HttpExchange
@ClientRegistrationId(AO_OPPFOLGINGSKONTOR_CLIENT_ID)
interface OppfolgingskontorApi {
    @PostExchange("/graphql")
    fun hentKontorForBruker(
        @RequestBody request: GraphQLRequest,
    ): GraphQLResponse<HentKontorerResponse>

    data class GraphQLRequest(
        val query: String,
        val variables: Map<String, String> = emptyMap(),
    )

    data class GraphQLResponse<T>(
        val data: T? = null,
        val errors: List<GraphQLError>? = null,
    )

    data class GraphQLError(
        val message: String,
    )

    data class HentKontorerResponse(
        val kontorTilhorigheter: KontorTilhorigheter,
    )

    data class KontorTilhorigheter(
        val arbeidsoppfolging: Arbeidsoppfolging? = null,
    )
}
