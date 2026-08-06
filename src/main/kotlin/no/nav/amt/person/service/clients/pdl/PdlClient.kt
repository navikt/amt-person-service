package no.nav.amt.person.service.clients.pdl

import no.nav.amt.person.service.person.model.AdressebeskyttelseGradering
import no.nav.amt.person.service.person.model.IdentType
import no.nav.amt.person.service.person.model.Personident
import no.nav.amt.person.service.poststed.PoststedRepository
import no.nav.amt.person.service.utils.GraphqlUtils
import no.nav.amt.person.service.utils.GraphqlUtils.GraphqlQuery
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class PdlClient(
    private val pdlApi: PdlApi,
    private val poststedRepository: PoststedRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val EMPTY_DATA_MSG = "PDL respons inneholder ikke data"
    }

    fun hentPerson(personident: String): PdlPerson {
        val response = pdlApi.hentPerson(graphqlQuery(PdlQueries.HentPerson.query, personident))
        val data = validateAndGetData(response, response.extensions)
        return data.toPdlBruker { postnummer -> poststedRepository.getPoststeder(postnummer) }
    }

    fun hentPersonFodselsar(personident: String): Int {
        val response = pdlApi.hentPersonFodselsar(graphqlQuery(PdlQueries.HentPersonFodselsar.query, personident))
        val data = validateAndGetData(response, response.extensions)
        return data.hentPerson.foedselsdato
            .firstOrNull()
            ?.foedselsaar
            ?: throw RuntimeException("PDL person mangler fodselsdato")
    }

    fun hentIdenter(personident: String): List<Personident> {
        val response = pdlApi.hentIdenter(graphqlQuery(PdlQueries.HentIdenter.query, personident))
        val data = validateAndGetData(response, response.extensions)
        val hentIdenter = data.hentIdenter ?: throw RuntimeException(EMPTY_DATA_MSG)
        return hentIdenter.identer.map {
            Personident(
                ident = it.ident,
                historisk = it.historisk,
                type = IdentType.valueOf(it.gruppe),
            )
        }
    }

    fun hentTelefon(personident: String): String? {
        val response = pdlApi.hentTelefon(graphqlQuery(PdlQueries.HentTelefon.query, personident))
        val data = validateAndGetData(response, response.extensions)
        return data.hentPerson.telefonnummer.toTelefonnummer()
    }

    fun hentAdressebeskyttelse(personident: String): AdressebeskyttelseGradering? {
        val response = pdlApi.hentAdressebeskyttelse(graphqlQuery(PdlQueries.HentAdressebeskyttelse.query, personident))
        val data = validateAndGetData(response, response.extensions)
        return data.hentPerson.adressebeskyttelse.toDiskresjonskode()
    }

    private fun graphqlQuery(
        query: String,
        personident: String,
    ) = GraphqlQuery(query, PdlQueries.Variables(personident))

    private fun <Data> validateAndGetData(
        response: GraphqlUtils.GraphqlResponse<Data, PdlQueries.PdlErrorExtension>,
        extensions: PdlQueries.Extensions?,
    ): Data {
        throwPdlApiErrors(response)
        logPdlWarnings(extensions?.warnings)
        return response.data ?: throw RuntimeException(EMPTY_DATA_MSG)
    }

    private fun throwPdlApiErrors(response: GraphqlUtils.GraphqlResponse<*, PdlQueries.PdlErrorExtension>) {
        response.errors?.takeIf { it.isNotEmpty() }?.let { feilmeldinger ->
            val melding = buildString {
                append("Feilmeldinger i respons fra pdl:\n")
                if (response.data == null) append("- data i respons er null \n")
                feilmeldinger.forEach {
                    append("- ${it.message} (code: ${it.extensions?.code} details: ${it.extensions?.details})\n")
                }
            }
            throw RuntimeException(melding)
        }
    }

    private fun logPdlWarnings(warnings: List<PdlQueries.PdlWarning>?) {
        if (warnings == null) return
        val stringBuilder = StringBuilder("Respons fra Pdl inneholder warnings:\n")
        warnings.forEach {
            stringBuilder.append(
                "query: ${it.query},\n" + "id: ${it.id},\n" + "message: ${it.message},\n" + "details: ${it.details}\n",
            )
        }
        log.warn(stringBuilder.toString())
    }
}
