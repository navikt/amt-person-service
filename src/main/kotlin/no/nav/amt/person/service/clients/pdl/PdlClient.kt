package no.nav.amt.person.service.clients.pdl

import no.nav.amt.person.service.clients.HeaderConstants.BEHANDLINGSNUMMER_HEADER
import no.nav.amt.person.service.clients.HeaderConstants.GEN_TEMA_HEADER_VALUE
import no.nav.amt.person.service.clients.HeaderConstants.TEMA_HEADER
import no.nav.amt.person.service.person.model.AdressebeskyttelseGradering
import no.nav.amt.person.service.person.model.IdentType
import no.nav.amt.person.service.person.model.Personident
import no.nav.amt.person.service.poststed.PoststedRepository
import no.nav.amt.person.service.utils.GraphqlUtils
import no.nav.amt.person.service.utils.GraphqlUtils.GraphqlResponse
import no.nav.amt.person.service.utils.OkHttpClientUtils.mediaTypeJson
import no.nav.common.rest.client.RestClient.baseClient
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue

class PdlClient(
	private val baseUrl: String,
	private val tokenProvider: () -> String,
	private val httpClient: OkHttpClient = baseClient(),
	private val poststedRepository: PoststedRepository,
	private val objectMapper: ObjectMapper,
) {
	private val log = LoggerFactory.getLogger(javaClass)

	companion object {
		private const val BEHANDLINGSNUMMER =
			"B446" // https://behandlingskatalog.nais.adeo.no/process/team/5345bce7-e076-4b37-8bf4-49030901a4c3/b3003849-c4bb-4c60-a4cb-e07ce6025623
	}

	fun hentPerson(personident: String): PdlPerson {
		val requestBody =
			objectMapper.writeValueAsString(
				GraphqlUtils.GraphqlQuery(
					PdlQueries.HentPerson.query,
					PdlQueries.Variables(personident),
				),
			)

		val request = createGraphqlRequest(requestBody)

		httpClient.newCall(request).execute().use { response ->
			if (!response.isSuccessful) {
				throw RuntimeException("Klarte ikke å hente informasjon fra PDL. Status: ${response.code}")
			}

			val gqlResponse = objectMapper.readValue<PdlQueries.HentPerson.Response>(response.body.string())

			// respons kan inneholde feil selv om den ikke er tom
			// ref: https://pdldocs-navno.msappproxy.net/ekstern/index.html#appendix-graphql-feilhandtering
			throwPdlApiErrors(gqlResponse)

			logPdlWarnings(gqlResponse.extensions?.warnings)

			if (gqlResponse.data == null) {
				throw RuntimeException("PDL respons inneholder ikke data")
			}

			val pdlPerson =
				gqlResponse.data.toPdlBruker { postnummer -> poststedRepository.getPoststeder(postnummer) }

			if (pdlPerson.erUkjent()) {
				log.warn("PDL-person har ukjent etternavn")
			}

			return pdlPerson
		}
	}

	fun hentPersonFodselsar(personident: String): Int {
		val requestBody =
			objectMapper.writeValueAsString(
				GraphqlUtils.GraphqlQuery(
					PdlQueries.HentPersonFodselsar.query,
					PdlQueries.Variables(personident),
				),
			)

		val request = createGraphqlRequest(requestBody)

		httpClient.newCall(request).execute().use { response ->
			if (!response.isSuccessful) {
				throw RuntimeException("Klarte ikke å hente informasjon fra PDL. Status: ${response.code}")
			}

			val gqlResponse = objectMapper.readValue<PdlQueries.HentPersonFodselsar.Response>(response.body.string())

			// respons kan inneholde feil selv om den ikke er tom
			// ref: https://pdldocs-navno.msappproxy.net/ekstern/index.html#appendix-graphql-feilhandtering
			throwPdlApiErrors(gqlResponse)

			logPdlWarnings(gqlResponse.extensions?.warnings)

			if (gqlResponse.data == null) {
				throw RuntimeException("PDL respons inneholder ikke data")
			}

			val fodselsdato =
				gqlResponse.data.hentPerson.foedselsdato
					.firstOrNull()
					?: throw RuntimeException("PDL person mangler fodselsdato")
			return fodselsdato.foedselsaar
		}
	}

	fun hentIdenter(ident: String): List<Personident> {
		val requestBody =
			objectMapper.writeValueAsString(
				GraphqlUtils.GraphqlQuery(
					PdlQueries.HentIdenter.query,
					PdlQueries.Variables(ident),
				),
			)

		val request = createGraphqlRequest(requestBody)

		httpClient.newCall(request).execute().use { response ->
			if (!response.isSuccessful) {
				throw RuntimeException("Klarte ikke å hente informasjon fra PDL. Status: ${response.code}")
			}

			val gqlResponse = objectMapper.readValue<PdlQueries.HentIdenter.Response>(response.body.string())

			throwPdlApiErrors(gqlResponse)

			logPdlWarnings(gqlResponse.extensions?.warnings)

			if (gqlResponse.data?.hentIdenter == null) {
				throw RuntimeException("PDL respons inneholder ikke data")
			}

			return gqlResponse.data.hentIdenter.identer.map {
				Personident(
					it.ident,
					it.historisk,
					IdentType.valueOf(it.gruppe),
				)
			}
		}
	}

	fun hentTelefon(ident: String): String? {
		val requestBody =
			objectMapper.writeValueAsString(
				GraphqlUtils.GraphqlQuery(
					PdlQueries.HentTelefon.query,
					PdlQueries.Variables(ident),
				),
			)

		val request = createGraphqlRequest(requestBody)

		httpClient.newCall(request).execute().use { response ->
			if (!response.isSuccessful) {
				throw RuntimeException("Klarte ikke å hente informasjon fra PDL. Status: ${response.code}")
			}

			val gqlResponse = objectMapper.readValue<PdlQueries.HentTelefon.Response>(response.body.string())

			throwPdlApiErrors(gqlResponse)
			logPdlWarnings(gqlResponse.extensions?.warnings)

			if (gqlResponse.data == null) {
				throw RuntimeException("PDL respons inneholder ikke data")
			}

			return gqlResponse.data.hentPerson.telefonnummer
				.toTelefonnummer()
		}
	}

	fun hentAdressebeskyttelse(personident: String): AdressebeskyttelseGradering? {
		val requestBody =
			objectMapper.writeValueAsString(
				GraphqlUtils.GraphqlQuery(
					PdlQueries.HentAdressebeskyttelse.query,
					PdlQueries.Variables(personident),
				),
			)

		val request = createGraphqlRequest(requestBody)

		httpClient.newCall(request).execute().use { response ->
			if (!response.isSuccessful) {
				throw RuntimeException("Klarte ikke å hente informasjon fra PDL. Status: ${response.code}")
			}

			val gqlResponse = objectMapper.readValue<PdlQueries.HentAdressebeskyttelse.Response>(response.body.string())

			throwPdlApiErrors(gqlResponse)
			logPdlWarnings(gqlResponse.extensions?.warnings)

			if (gqlResponse.data == null) {
				throw RuntimeException("PDL respons inneholder ikke data")
			}

			return gqlResponse.data.hentPerson.adressebeskyttelse
				.toDiskresjonskode()
		}
	}

	private fun createGraphqlRequest(jsonPayload: String): Request =
		Request
			.Builder()
			.url("$baseUrl/graphql")
			.addHeader(HttpHeaders.AUTHORIZATION, "Bearer ${tokenProvider()}")
			.addHeader(TEMA_HEADER, GEN_TEMA_HEADER_VALUE)
			.addHeader(BEHANDLINGSNUMMER_HEADER, BEHANDLINGSNUMMER)
			.post(jsonPayload.toRequestBody(mediaTypeJson))
			.build()

	private fun throwPdlApiErrors(response: GraphqlResponse<*, PdlQueries.PdlErrorExtension>) {
		var melding = "Feilmeldinger i respons fra pdl:\n"
		if (response.data == null) melding = "$melding- data i respons er null \n"
		response.errors?.let { feilmeldinger ->
			melding +=
				feilmeldinger.joinToString(separator = "") { "- ${it.message} (code: ${it.extensions?.code} details: ${it.extensions?.details})\n" }
			throw RuntimeException(melding)
		}
	}

	private fun logPdlWarnings(warnings: List<PdlQueries.PdlWarning>?) {
		if (warnings == null) return
		val stringBuilder = StringBuilder("Respons fra Pdl inneholder warnings:\n")
		warnings.forEach {
			stringBuilder.append(
				"query: ${it.query},\n" +
					"id: ${it.id},\n" +
					"message: ${it.message},\n" +
					"details: ${it.details}\n",
			)
		}

		log.warn(stringBuilder.toString())
	}
}
