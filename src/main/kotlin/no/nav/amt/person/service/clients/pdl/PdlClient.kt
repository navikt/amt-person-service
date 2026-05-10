package no.nav.amt.person.service.clients.pdl

import no.nav.amt.person.service.clients.HeaderConstants.BEHANDLINGSNUMMER_HEADER
import no.nav.amt.person.service.clients.HeaderConstants.GEN_TEMA_HEADER_VALUE
import no.nav.amt.person.service.clients.HeaderConstants.NAV_CONSUMER_ID_HEADER
import no.nav.amt.person.service.clients.HeaderConstants.NAV_CONSUMER_ID_HEADER_VALUE
import no.nav.amt.person.service.clients.HeaderConstants.TEMA_HEADER
import no.nav.amt.person.service.person.model.AdressebeskyttelseGradering
import no.nav.amt.person.service.person.model.IdentType
import no.nav.amt.person.service.person.model.Personident
import no.nav.amt.person.service.poststed.PoststedRepository
import no.nav.amt.person.service.utils.GraphqlUtils
import no.nav.amt.person.service.utils.GraphqlUtils.GraphqlResponse
import no.nav.common.token_client.client.MachineToMachineTokenClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient

@Service
class PdlClient(
    @Value($$"${pdl.url}") url: String,
    @Value($$"${pdl.scope}") private val scope: String,
    restClientBuilder: RestClient.Builder,
    private val machineToMachineTokenClient: MachineToMachineTokenClient,
    private val poststedRepository: PoststedRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val BEHANDLINGSNUMMER =
            // https://behandlingskatalog.nais.adeo.no/process/team/5345bce7-e076-4b37-8bf4-49030901a4c3/b3003849-c4bb-4c60-a4cb-e07ce6025623
            "B446"

        private const val EMPTY_DATA_MSG = "PDL respons inneholder ikke data"
    }

    private val restClient: RestClient = restClientBuilder
        .baseUrl("$url/graphql")
        .defaultHeader(TEMA_HEADER, GEN_TEMA_HEADER_VALUE)
        .defaultHeader(BEHANDLINGSNUMMER_HEADER, BEHANDLINGSNUMMER)
        .defaultHeader(NAV_CONSUMER_ID_HEADER, NAV_CONSUMER_ID_HEADER_VALUE)
        .defaultRequest {
            it.header(HttpHeaders.AUTHORIZATION, "Bearer ${machineToMachineTokenClient.createMachineToMachineToken(scope)}")
        }.build()

    fun hentPerson(personident: String): PdlPerson {
        val response = executeQuery<PdlQueries.HentPerson.Response>(
            query = PdlQueries.HentPerson.query,
            personident = personident,
        )

        val data = validateAndGetData(response, response.extensions)
        return data.toPdlBruker { postnummer -> poststedRepository.getPoststeder(postnummer) }
    }

    fun hentPersonFodselsar(personident: String): Int {
        val response = executeQuery<PdlQueries.HentPersonFodselsar.Response>(
            PdlQueries.HentPersonFodselsar.query,
            personident,
        )

        val data = validateAndGetData(response, response.extensions)
        return data.hentPerson.foedselsdato
            .firstOrNull()
            ?.foedselsaar
            ?: throw RuntimeException("PDL person mangler fodselsdato")
    }

    fun hentIdenter(personident: String): List<Personident> {
        val response = executeQuery<PdlQueries.HentIdenter.Response>(
            PdlQueries.HentIdenter.query,
            personident,
        )

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
        val response = executeQuery<PdlQueries.HentTelefon.Response>(
            PdlQueries.HentTelefon.query,
            personident,
        )

        val data = validateAndGetData(response, response.extensions)
        return data.hentPerson.telefonnummer.toTelefonnummer()
    }

    fun hentAdressebeskyttelse(personident: String): AdressebeskyttelseGradering? {
        val response = executeQuery<PdlQueries.HentAdressebeskyttelse.Response>(
            PdlQueries.HentAdressebeskyttelse.query,
            personident,
        )

        val data = validateAndGetData(response, response.extensions)
        return data.hentPerson.adressebeskyttelse.toDiskresjonskode()
    }

    private inline fun <reified T : Any> executeQuery(
        query: String,
        personident: String,
    ): T = restClient
        .post()
        .body(GraphqlUtils.GraphqlQuery(query, PdlQueries.Variables(personident)))
        .retrieve()
        .body(T::class.java) ?: throw RuntimeException("Tomt svar fra PDL")

    private fun <Data> validateAndGetData(
        response: GraphqlResponse<Data, PdlQueries.PdlErrorExtension>,
        extensions: PdlQueries.Extensions?,
    ): Data {
        throwPdlApiErrors(response)
        logPdlWarnings(extensions?.warnings)
        return response.data ?: throw RuntimeException(EMPTY_DATA_MSG)
    }

    private fun throwPdlApiErrors(response: GraphqlResponse<*, PdlQueries.PdlErrorExtension>) {
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
