package no.nav.amt.person.service.clients.nom

import no.nav.amt.person.service.clients.GraphqlRequest
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.treeToValue

@Service
class NomClient(
    private val nomApi: NomApi,
    private val objectMapper: ObjectMapper,
) {
    fun hentNavAnsatt(navIdent: String): NomNavAnsatt? = hentNavAnsatte(listOf(navIdent))
        .firstOrNull()
        .also { if (it == null) log.info("Fant ikke veileder i NOM med ident $navIdent") }

    fun hentNavAnsatte(navIdenter: List<String>): List<NomNavAnsatt> {
        val response = nomApi.execute(GraphqlRequest(hentRessurserQuery, mapOf(QUERY_IDENTER to navIdenter)))

        val data = response[DATA] ?: return emptyList()
        if (data.isNull) return emptyList()

        val ressurserNode = data[RESSURSER] ?: return emptyList()
        val ressurser: List<NomQueries.RessursResult> = objectMapper.treeToValue(ressurserNode)

        return ressurser.mapNotNull { result ->
            if (result.code != NomQueries.ResultCode.OK || result.ressurs == null) {
                log.warn("Fant ikke veileder i NOM. statusCode=${result.code}")
                return@mapNotNull null
            }
            val ansatt = result.ressurs
            NomNavAnsatt(
                navIdent = ansatt.navident,
                navn = ansatt.visningsnavn ?: "${ansatt.fornavn} ${ansatt.etternavn}",
                epost = ansatt.epost,
                telefonnummer = hentTjenesteTelefonnummer(ansatt),
                orgTilknytning = ansatt.orgTilknytning,
            )
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(NomClient::class.java)
        private const val QUERY_IDENTER = "identer"
        private const val DATA = "data"
        private const val RESSURSER = "ressurser"

        private val hentRessurserQuery =
            ClassPathResource("graphql-documents/hentRessurser.graphql").getContentAsString(Charsets.UTF_8)

        private fun hentTjenesteTelefonnummer(ansatt: NomQueries.Ressurs): String? =
            ansatt.telefon.find { it.type == "NAV_KONTOR_TELEFON" }?.nummer
                ?: ansatt.telefon.find { it.type == "NAV_TJENESTE_TELEFON" }?.nummer
                ?: ansatt.primaryTelefon
    }
}
