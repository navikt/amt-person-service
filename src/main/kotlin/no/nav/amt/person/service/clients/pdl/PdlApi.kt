package no.nav.amt.person.service.clients.pdl

import no.nav.amt.person.service.clients.PDL_API_CLIENT_ID
import no.nav.amt.person.service.utils.GraphqlUtils.GraphqlQuery
import org.springframework.security.oauth2.client.annotation.ClientRegistrationId
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.service.annotation.HttpExchange
import org.springframework.web.service.annotation.PostExchange

@HttpExchange
@ClientRegistrationId(PDL_API_CLIENT_ID)
interface PdlApi {
    @PostExchange("/graphql")
    fun hentPerson(
        @RequestBody query: GraphqlQuery,
    ): PdlQueries.HentPerson.Response

    @PostExchange("/graphql")
    fun hentPersonFodselsar(
        @RequestBody query: GraphqlQuery,
    ): PdlQueries.HentPersonFodselsar.Response

    @PostExchange("/graphql")
    fun hentIdenter(
        @RequestBody query: GraphqlQuery,
    ): PdlQueries.HentIdenter.Response

    @PostExchange("/graphql")
    fun hentTelefon(
        @RequestBody query: GraphqlQuery,
    ): PdlQueries.HentTelefon.Response

    @PostExchange("/graphql")
    fun hentAdressebeskyttelse(
        @RequestBody query: GraphqlQuery,
    ): PdlQueries.HentAdressebeskyttelse.Response
}
