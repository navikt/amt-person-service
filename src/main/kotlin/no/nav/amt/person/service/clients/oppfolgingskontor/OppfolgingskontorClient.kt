package no.nav.amt.person.service.clients.oppfolgingskontor

import no.nav.amt.person.service.utils.OkHttpClientUtils.mediaTypeJson
import no.nav.common.rest.client.RestClient.baseClient
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.springframework.http.HttpHeaders
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue

class OppfolgingskontorClient(
    private val baseUrl: String,
    private val tokenProvider: () -> String,
    private val objectMapper: ObjectMapper,
    private val httpClient: OkHttpClient = baseClient(),
) {
    private val kontorForBrukerQuery =
        $$"""
        query HentKontorer($ident: String!) {
          kontorTilhorigheter(ident: $ident) {
            arbeidsoppfolging {
                kontorId   
                kontorNavn 
            }
          }
        }
        """.trimIndent()

    fun hentKontorForBruker(ident: String): Arbeidsoppfolging? {
        val requestBody = objectMapper.writeValueAsString(
            GraphQLRequest(
                query = kontorForBrukerQuery,
                variables = mapOf("ident" to ident),
            ),
        )

        val request = Request
            .Builder()
            .url("$baseUrl/graphql")
            .addHeader(HttpHeaders.AUTHORIZATION, "Bearer ${tokenProvider()}")
            .post(requestBody.toRequestBody(mediaTypeJson))
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw RuntimeException("Klarte ikke å hente kontor fra ao-oppfolgingskontor. Status: ${response.code}")
            }

            val gqlResponse = objectMapper.readValue<GraphQLResponse<HentKontorerResponse>>(response.body.string())

            gqlResponse.errors?.takeIf { it.isNotEmpty() }?.let { errors ->
                val melding = errors.joinToString(separator = "\n") { "- ${it.message}" }
                throw RuntimeException("Feilmeldinger i respons fra ao-oppfolgingskontor:\n$melding")
            }

            if (gqlResponse.data == null) {
                throw RuntimeException("ao-oppfolgingskontor respons inneholder ikke data")
            }

            return gqlResponse.data.kontorTilhorigheter.arbeidsoppfolging
        }
    }
}

data class GraphQLRequest(
    val query: String,
    val variables: Map<String, String> = emptyMap(),
)

data class GraphQLResponse<T>(
    val data: T? = null,
    val errors: List<GraphQLError>? = null,
)

data class GraphQLError(
    val message: String,
)

data class HentKontorerResponse(
    val kontorTilhorigheter: KontorTilhorigheter,
)

data class KontorTilhorigheter(
    val arbeidsoppfolging: Arbeidsoppfolging? = null,
)

data class Arbeidsoppfolging(
    val kontorId: String,
    val kontorNavn: String,
)
