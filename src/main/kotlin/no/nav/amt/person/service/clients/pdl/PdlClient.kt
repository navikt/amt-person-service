package no.nav.amt.person.service.clients.pdl

import no.nav.amt.person.service.clients.GraphqlRequest
import no.nav.amt.person.service.clients.GraphqlResponse
import no.nav.amt.person.service.person.model.AdressebeskyttelseGradering
import no.nav.amt.person.service.person.model.IdentType
import no.nav.amt.person.service.person.model.Personident
import no.nav.amt.person.service.poststed.PoststedRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper

@Service
class PdlClient(
    private val pdlApi: PdlApi,
    private val poststedRepository: PoststedRepository,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun hentPerson(personident: String): PdlPerson {
        val response = executeQuery(hentPersonQuery, personident)

        val hentPerson: PdlQueries.HentPersonResult = response.requiredDataAt(HENT_PERSON)
        val hentIdenter: PdlQueries.HentIdenterResult = response.requiredDataAt(HENT_IDENTER)

        return toPdlBruker(hentPerson, hentIdenter) { postnummer -> poststedRepository.getPoststeder(postnummer) }
    }

    fun hentPersonFodselsar(personident: String): Int {
        val response = executeQuery(hentPersonFodselsarQuery, personident)

        val hentPerson: PdlQueries.HentPersonFoedselsdatoResult = response.requiredDataAt(HENT_PERSON)

        return hentPerson.foedselsdato.firstOrNull()?.foedselsaar
            ?: throw RuntimeException("PDL person mangler fodselsdato")
    }

    fun hentIdenter(personident: String): List<Personident> {
        val response = executeQuery(hentIdenterQuery, personident)

        val hentIdenter: PdlQueries.HentIdenterResult = response.requiredDataAt(HENT_IDENTER)

        return hentIdenter.identer.map {
            Personident(
                ident = it.ident,
                historisk = it.historisk,
                type = IdentType.valueOf(it.gruppe),
            )
        }
    }

    fun hentTelefon(personident: String): String? {
        val response = executeQuery(hentTelefonQuery, personident)

        val hentPerson: HentTelefonResult = response.requiredDataAt(HENT_PERSON)

        return hentPerson.telefonnummer.toTelefonnummer()
    }

    fun hentAdressebeskyttelse(personident: String): AdressebeskyttelseGradering? {
        val response = executeQuery(hentAdressebeskyttelseQuery, personident)

        val hentPerson: HentAdressebeskyttelseResult = response.requiredDataAt(HENT_PERSON)

        return hentPerson.adressebeskyttelse.toDiskresjonskode()
    }

    private fun executeQuery(
        query: String,
        personident: String,
    ): GraphqlResponse {
        val jsonResponse = pdlApi.execute(GraphqlRequest(query, PdlQueries.Variables(personident)))
        val response = GraphqlResponse(jsonResponse, objectMapper, "PDL")
        handlePdlErrors(response)
        logPdlWarnings(response)
        return response
    }

    private fun handlePdlErrors(response: GraphqlResponse) {
        val errors = response.errors ?: return

        val melding = buildString {
            append("Feilmeldinger i respons fra pdl:\n")
            if (response.data == null) append("- data i respons er null \n")
            errors.forEach { error ->
                val extensions = error["extensions"]
                val code = extensions?.get("code")?.asString()
                val detailsNode = extensions?.get("details")
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

    private fun logPdlWarnings(response: GraphqlResponse) {
        val warnings = response.extensions?.get("warnings")?.takeIf { !it.isNull && it.isArray && !it.isEmpty } ?: return
        val melding = buildString {
            append("Respons fra Pdl inneholder warnings:\n")
            warnings.forEach { warning ->
                append(
                    "query: ${warning["query"]?.asString()},\n" +
                        "id: ${warning["id"]?.asString()},\n" +
                        "message: ${warning["message"]?.asString()},\n" +
                        "details: ${warning["details"]?.asString()}\n",
                )
            }
        }
        log.warn(melding)
    }

    private data class HentTelefonResult(
        val telefonnummer: List<PdlQueries.Attribute.Telefonnummer>,
    )

    private data class HentAdressebeskyttelseResult(
        val adressebeskyttelse: List<PdlQueries.Attribute.Adressebeskyttelse>,
    )

    companion object {
        private const val HENT_PERSON = "hentPerson"
        private const val HENT_IDENTER = "hentIdenter"

        private val hentPersonQuery = GraphqlResponse.loadDocument("hentPerson")
        private val hentPersonFodselsarQuery = GraphqlResponse.loadDocument("hentPersonFodselsar")
        private val hentIdenterQuery = GraphqlResponse.loadDocument("hentIdenter")
        private val hentTelefonQuery = GraphqlResponse.loadDocument("hentTelefon")
        private val hentAdressebeskyttelseQuery = GraphqlResponse.loadDocument("hentAdressebeskyttelse")
    }
}
