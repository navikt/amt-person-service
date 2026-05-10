package no.nav.amt.person.service.clients.nom

import no.nav.amt.person.service.clients.HeaderConstants
import no.nav.amt.person.service.utils.GraphqlUtils
import no.nav.common.token_client.client.MachineToMachineTokenClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.body

@Service
class NomClient(
    @Value($$"${nom.url}") url: String,
    @Value($$"${nom.scope}") private val scope: String,
    restClientBuilder: RestClient.Builder,
    private val machineToMachineTokenClient: MachineToMachineTokenClient,
) {
    private val restClient: RestClient = restClientBuilder
        .baseUrl(url)
        .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        .defaultHeader(HeaderConstants.NAV_CONSUMER_ID_HEADER, "amt-person-service")
        .defaultRequest {
            it.header(HttpHeaders.AUTHORIZATION, "Bearer ${machineToMachineTokenClient.createMachineToMachineToken(scope)}")
        }.build()

    fun hentNavAnsatt(navIdent: String): NomNavAnsatt? = hentNavAnsatte(listOf(navIdent))
        .firstOrNull()
        .also { if (it == null) log.info("Fant ikke veileder i NOM med ident $navIdent") }

    fun hentNavAnsatte(navIdenter: List<String>): List<NomNavAnsatt> {
        val ressurserResponse = restClient
            .post()
            .uri("/graphql")
            .body(
                GraphqlUtils.GraphqlQuery(
                    NomQueries.HentRessurser.query,
                    NomQueries.HentRessurser.Variables(navIdenter),
                ),
            ).retrieve()
            .body<NomQueries.HentRessurser.Response>()
            ?: throw RuntimeException("Tomt svar fra NOM")

        return ressurserResponse.toVeiledere()
    }

    companion object {
        private val log = LoggerFactory.getLogger(NomClient::class.java)

        private fun hentTjenesteTelefonnummer(ansatt: NomQueries.HentRessurser.Ressurs): String? =
            ansatt.telefon.find { it.type == "NAV_KONTOR_TELEFON" }?.nummer
                ?: ansatt.telefon.find { it.type == "NAV_TJENESTE_TELEFON" }?.nummer
                ?: ansatt.primaryTelefon

        private fun NomQueries.HentRessurser.Response.toVeiledere(): List<NomNavAnsatt> = this.data?.ressurser?.mapNotNull {
            if (it.code != NomQueries.HentRessurser.ResultCode.OK || it.ressurs == null) {
                log.warn("Fant ikke veileder i NOM. statusCode=${it.code}")
                return@mapNotNull null
            }

            val telefonnummer = hentTjenesteTelefonnummer(it.ressurs)
            val ansatt = it.ressurs

            NomNavAnsatt(
                navIdent = ansatt.navident,
                navn = ansatt.visningsnavn ?: "${ansatt.fornavn} ${ansatt.etternavn}",
                epost = it.ressurs.epost,
                telefonnummer = telefonnummer,
                orgTilknytning = ansatt.orgTilknytning,
            )
        } ?: emptyList()
    }
}
