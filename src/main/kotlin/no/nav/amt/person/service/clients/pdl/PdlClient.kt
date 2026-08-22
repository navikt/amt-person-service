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
import tools.jackson.module.kotlin.treeToValue

@Service
class PdlClient(
    private val pdlApi: PdlApi,
    private val poststedRepository: PoststedRepository,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val EMPTY_DATA_MSG = "PDL respons inneholder ikke data"
        private const val HENT_PERSON_DOCUMENT = "hentPerson"
        private const val HENT_PERSON_FODSELSAAR_DOCUMENT = "hentPersonFodselsar"
        private const val HENT_IDENTER_DOCUMENT = "hentIdenter"
        private const val HENT_TELEFON_DOCUMENT = "hentTelefon"
        private const val HENT_ADRESSEBESKYTTELSE_DOCUMENT = "hentAdressebeskyttelse"

        private const val DATA = "data"
        private const val ERRORS = "errors"
        private const val EXTENSIONS = "extensions"
        private const val WARNINGS = "warnings"
        private const val HENT_PERSON = "hentPerson"
        private const val HENT_IDENTER = "hentIdenter"
        private const val CODE = "code"
        private const val DETAILS = "details"
        private const val TYPE = "type"
        private const val CAUSE = "cause"
        private const val POLICY = "policy"
        private const val MESSAGE = "message"
        private const val QUERY = "query"
        private const val ID = "id"

        private val hentPersonQuery = loadQuery(HENT_PERSON_DOCUMENT)
        private val hentPersonFodselsarQuery = loadQuery(HENT_PERSON_FODSELSAAR_DOCUMENT)
        private val hentIdenterQuery = loadQuery(HENT_IDENTER_DOCUMENT)
        private val hentTelefonQuery = loadQuery(HENT_TELEFON_DOCUMENT)
        private val hentAdressebeskyttelseQuery = loadQuery(HENT_ADRESSEBESKYTTELSE_DOCUMENT)

        private fun loadQuery(name: String) = ClassPathResource("graphql-documents/$name.graphql").getContentAsString(Charsets.UTF_8)
    }

    fun hentPerson(personident: String): PdlPerson {
        val response = pdlApi.execute(GraphqlRequest(hentPersonQuery, PdlQueries.Variables(personident)))
        handlePdlErrors(response)
        logPdlWarnings(response)

        val data = requiredData(response)
        val hentPerson: PdlQueries.HentPersonResult = objectMapper.treeToValue(data[HENT_PERSON])
        val hentIdenter: PdlQueries.HentIdenterResult = objectMapper.treeToValue(data[HENT_IDENTER])

        return toPdlBruker(hentPerson, hentIdenter) { postnummer -> poststedRepository.getPoststeder(postnummer) }
    }

    fun hentPersonFodselsar(personident: String): Int {
        val response = pdlApi.execute(GraphqlRequest(hentPersonFodselsarQuery, PdlQueries.Variables(personident)))
        handlePdlErrors(response)
        logPdlWarnings(response)

        val data = requiredData(response)
        val hentPerson: PdlQueries.HentPersonFoedselsdatoResult = objectMapper.treeToValue(data[HENT_PERSON])

        return hentPerson.foedselsdato.firstOrNull()?.foedselsaar
            ?: throw RuntimeException("PDL person mangler fodselsdato")
    }

    fun hentIdenter(personident: String): List<Personident> {
        val response = pdlApi.execute(GraphqlRequest(hentIdenterQuery, PdlQueries.Variables(personident)))
        handlePdlErrors(response)
        logPdlWarnings(response)

        val data = requiredData(response)
        val hentIdenterNode = data[HENT_IDENTER]
        if (hentIdenterNode == null || hentIdenterNode.isNull) throw RuntimeException(EMPTY_DATA_MSG)

        val hentIdenter: PdlQueries.HentIdenterResult = objectMapper.treeToValue(hentIdenterNode)

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
        val hentPerson: HentTelefonResult = objectMapper.treeToValue(data[HENT_PERSON])

        return hentPerson.telefonnummer.toTelefonnummer()
    }

    fun hentAdressebeskyttelse(personident: String): AdressebeskyttelseGradering? {
        val response = pdlApi.execute(GraphqlRequest(hentAdressebeskyttelseQuery, PdlQueries.Variables(personident)))
        handlePdlErrors(response)
        logPdlWarnings(response)

        val data = requiredData(response)
        val hentPerson: HentAdressebeskyttelseResult = objectMapper.treeToValue(data[HENT_PERSON])

        return hentPerson.adressebeskyttelse.toDiskresjonskode()
    }

    private fun requiredData(response: JsonNode): JsonNode {
        val data = response[DATA]
        if (data == null || data.isNull) throw RuntimeException(EMPTY_DATA_MSG)
        return data
    }

    private fun handlePdlErrors(response: JsonNode) {
        val errors = response[ERRORS]?.takeIf { !it.isNull && it.isArray } ?: return
        if (errors.isEmpty) return

        val melding = buildString {
            append("Feilmeldinger i respons fra pdl:\n")
            val data = response[DATA]
            if (data == null || data.isNull) append("- data i respons er null \n")
            errors.forEach { error ->
                val code = error[EXTENSIONS]?.get(CODE)?.asString()
                val detailsNode = error[EXTENSIONS]?.get(DETAILS)
                val details = detailsNode?.takeIf { !it.isNull }?.let {
                    PdlQueries.PdlErrorDetails(
                        type = it[TYPE]?.asString(),
                        cause = it[CAUSE]?.asString(),
                        policy = it[POLICY]?.asString(),
                    )
                }
                append("- ${error[MESSAGE]?.asString()} (code: $code details: $details)\n")
            }
        }
        throw RuntimeException(melding)
    }

    private fun logPdlWarnings(response: JsonNode) {
        val warnings = response[EXTENSIONS]?.get(WARNINGS)?.takeIf { !it.isNull && it.isArray } ?: return
        if (warnings.isEmpty) return
        val stringBuilder = StringBuilder("Respons fra Pdl inneholder warnings:\n")
        warnings.forEach { warning ->
            stringBuilder.append(
                "query: ${warning[QUERY]?.asString()},\n" +
                    "id: ${warning[ID]?.asString()},\n" +
                    "message: ${warning[MESSAGE]?.asString()},\n" +
                    "details: ${warning[DETAILS]?.asString()}\n",
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
