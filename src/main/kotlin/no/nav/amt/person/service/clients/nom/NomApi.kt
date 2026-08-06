package no.nav.amt.person.service.clients.nom

import no.nav.amt.person.service.clients.NOM_API_CLIENT_ID
import no.nav.amt.person.service.utils.GraphqlUtils.GraphqlQuery
import org.springframework.security.oauth2.client.annotation.ClientRegistrationId
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.service.annotation.HttpExchange
import org.springframework.web.service.annotation.PostExchange

@HttpExchange
@ClientRegistrationId(NOM_API_CLIENT_ID)
interface NomApi {
    @PostExchange("/graphql")
    fun hentRessurser(
        @RequestBody query: GraphqlQuery,
    ): NomQueries.HentRessurser.Response
}
