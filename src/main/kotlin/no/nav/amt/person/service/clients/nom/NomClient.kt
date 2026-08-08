package no.nav.amt.person.service.clients.nom

import no.nav.amt.person.service.utils.GraphqlUtils.GraphqlQuery
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class NomClient(
    private val nomApi: NomApi,
) {
    fun hentNavAnsatt(navIdent: String): NomNavAnsatt? = hentNavAnsatte(listOf(navIdent))
        .firstOrNull()
        .also { if (it == null) log.info("Fant ikke veileder i NOM med ident $navIdent") }

    fun hentNavAnsatte(navIdenter: List<String>): List<NomNavAnsatt> {
        val ressurserResponse = nomApi.hentRessurser(
            GraphqlQuery(
                NomQueries.HentRessurser.query,
                NomQueries.HentRessurser.Variables(navIdenter),
            ),
        )
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
