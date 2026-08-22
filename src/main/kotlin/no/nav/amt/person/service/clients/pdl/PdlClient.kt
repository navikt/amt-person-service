package no.nav.amt.person.service.clients.pdl

import no.nav.amt.person.service.clients.GraphqlRequest
import no.nav.amt.person.service.person.model.AdressebeskyttelseGradering
import no.nav.amt.person.service.person.model.IdentType
import no.nav.amt.person.service.person.model.Personident
import no.nav.amt.person.service.poststed.PoststedRepository
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper

@Service
class PdlClient(
    private val pdlApi: PdlApi,
    private val poststedRepository: PoststedRepository,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val hentPersonQuery = loadQuery("hentPerson")
    private val hentPersonFodselsarQuery = loadQuery("hentPersonFodselsar")
    private val hentIdenterQuery = loadQuery("hentIdenter")
    private val hentTelefonQuery = loadQuery("hentTelefon")
    private val hentAdressebeskyttelseQuery = loadQuery("hentAdressebeskyttelse")

    companion object {
        private const val EMPTY_DATA_MSG = "PDL respons inneholder ikke data"

        private fun loadQuery(name: String) = ClassPathResource("graphql-documents/$name.graphql").getContentAsString(Charsets.UTF_8)
    }

    fun hentPerson(personident: String): PdlPerson {
        val response = pdlApi.execute(GraphqlRequest(hentPersonQuery, PdlQueries.Variables(personident)))
        handlePdlErrors(response)
        logPdlWarnings(response)

        val data = requiredData(response)
        val hentPerson = objectMapper.treeToValue(data["hentPerson"], PdlQueries.HentPersonResult::class.java)
            ?: throw RuntimeException(EMPTY_DATA_MSG)
        val hentIdenter = objectMapper.treeToValue(data["hentIdenter"], PdlQueries.HentIdenterResult::class.java)
            ?: throw RuntimeException(EMPTY_DATA_MSG)

        return toPdlBruker(hentPerson, hentIdenter) { postnummer -> poststedRepository.getPoststeder(postnummer) }
    }

    fun hentPersonFodselsar(personident: String): Int {
        val response = pdlApi.execute(GraphqlRequest(hentPersonFodselsarQuery, PdlQueries.Variables(personident)))
        handlePdlErrors(response)

        val data = requiredData(response)
        val hentPerson = objectMapper.treeToValue(data["hentPerson"], PdlQueries.HentPersonFoedselsdatoResult::class.java)
            ?: throw RuntimeException(EMPTY_DATA_MSG)

        return hentPerson.foedselsdato.firstOrNull()?.foedselsaar
            ?: throw RuntimeException("PDL person mangler fodselsdato")
    }

    fun hentIdenter(personident: String): List<Personident> {
        val response = pdlApi.execute(GraphqlRequest(hentIdenterQuery, PdlQueries.Variables(personident)))
        handlePdlErrors(response)

        val data = requiredData(response)
        val hentIdenterNode = data["hentIdenter"]
        if (hentIdenterNode == null || hentIdenterNode.isNull) throw RuntimeException(EMPTY_DATA_MSG)

        val hentIdenter = objectMapper.treeToValue(hentIdenterNode, PdlQueries.HentIdenterResult::class.java)
            ?: throw RuntimeException(EMPTY_DATA_MSG)

        return hentIdenter.identer.map {
            Personident(
                ident = it.ident,
                historisk = it.historisk,
                type = IdentType.valueOf(it.gruppe),
            )
        }
    }

    fun hentTelefon(personident: String): String? {
        val response = pdlApi.execute(GraphqlRequest(hentTelefonQuery, PdlQueries.Variables(personident)))
        handlePdlErrors(response)
        logPdlWarnings(response)

        val data = requiredData(response)
        val hentPerson = objectMapper.treeToValue(data["hentPerson"], HentTelefonResult::class.java)
            ?: throw RuntimeException(EMPTY_DATA_MSG)

        return hentPerson.telefonnummer.toTelefonnummer()
    }

    fun hentAdressebeskyttelse(personident: String): AdressebeskyttelseGradering? {
        val response = pdlApi.execute(GraphqlRequest(hentAdressebeskyttelseQuery, PdlQueries.Variables(personident)))
        handlePdlErrors(response)

        val data = requiredData(response)
        val hentPerson = objectMapper.treeToValue(data["hentPerson"], HentAdressebeskyttelseResult::class.java)
            ?: throw RuntimeException(EMPTY_DATA_MSG)

        return hentPerson.adressebeskyttelse.toDiskresjonskode()
    }

    private fun requiredData(response: JsonNode): JsonNode {
        val data = response["data"]
        if (data == null || data.isNull) throw RuntimeException(EMPTY_DATA_MSG)
        return data
    }

    private fun handlePdlErrors(response: JsonNode) {
        val errors = response["errors"]?.takeIf { !it.isNull && it.isArray } ?: return
        if (errors.isEmpty) return

        val melding = buildString {
            append("Feilmeldinger i respons fra pdl:\n")
            val data = response["data"]
            if (data == null || data.isNull) append("- data i respons er null \n")
            errors.forEach { error ->
                val code = error["extensions"]?.get("code")?.asString()
                val detailsNode = error["extensions"]?.get("details")
                val details = detailsNode?.takeIf { !it.isNull }?.let {
                    PdlQueries.PdlErrorDetails(
                        type = it["type"]?.asString(),
                        cause = it["cause"]?.asString(),
                        policy = it["policy"]?.asString(),
                    )
                }
                append("- ${error["message"]?.asString()} (code: $code details: $details)\n")
            }
        }
        throw RuntimeException(melding)
    }

    private fun logPdlWarnings(response: JsonNode) {
        val warnings = response["extensions"]?.get("warnings")?.takeIf { !it.isNull && it.isArray } ?: return
        if (warnings.isEmpty) return
        val stringBuilder = StringBuilder("Respons fra Pdl inneholder warnings:\n")
        warnings.forEach { warning ->
            stringBuilder.append(
                "query: ${warning["query"]?.asString()},\n" +
                    "id: ${warning["id"]?.asString()},\n" +
                    "message: ${warning["message"]?.asString()},\n" +
                    "details: ${warning["details"]?.asString()}\n",
            )
        }
        log.warn(stringBuilder.toString())
    }

    private data class HentTelefonResult(
        val telefonnummer: List<PdlQueries.Attribute.Telefonnummer>,
    )

    private data class HentAdressebeskyttelseResult(
        val adressebeskyttelse: List<PdlQueries.Attribute.Adressebeskyttelse>,
    )
}
