package no.nav.amt.person.service.clients.pdl

import no.nav.amt.person.service.person.model.AdressebeskyttelseGradering
import no.nav.amt.person.service.person.model.IdentType
import no.nav.amt.person.service.person.model.Personident
import no.nav.amt.person.service.utils.GraphqlUtils
import no.nav.amt.person.service.utils.GraphqlUtils.GraphqlResponse
import no.nav.amt.person.service.utils.JsonUtils.fromJsonString
import no.nav.amt.person.service.utils.JsonUtils.toJsonString
import no.nav.common.rest.client.RestClient.baseClient
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory
import java.util.function.Supplier

class PdlClient(
	private val baseUrl: String,
	private val tokenProvider: Supplier<String>,
	private val httpClient: OkHttpClient = baseClient(),
) {

	private val log = LoggerFactory.getLogger(javaClass)

	private val mediaTypeJson = "application/json".toMediaType()

	private val behandlingsnummer = "B446" // https://behandlingskatalog.nais.adeo.no/process/team/5345bce7-e076-4b37-8bf4-49030901a4c3/b3003849-c4bb-4c60-a4cb-e07ce6025623

	fun hentPerson(personIdent: String): PdlPerson {
		val requestBody = toJsonString(
			GraphqlUtils.GraphqlQuery(
				PdlQueries.HentPerson.query,
				PdlQueries.HentPerson.Variables(personIdent)
			)
		)

		val request = createGraphqlRequest(requestBody)

		httpClient.newCall(request).execute().use { response ->
			if (!response.isSuccessful) {
				throw RuntimeException("Klarte ikke å hente informasjon fra PDL. Status: ${response.code}")
			}

			val body = response.body?.string() ?: throw RuntimeException("Body is missing from PDL request")

			val gqlResponse = fromJsonString<PdlQueries.HentPerson.Response>(body)

			throwPdlApiErrors(gqlResponse) // respons kan inneholde feil selv om den ikke er tom ref: https://pdldocs-navno.msappproxy.net/ekstern/index.html#appendix-graphql-feilhandtering

			logPdlWarnings(gqlResponse.extensions?.warnings)

			if (gqlResponse.data == null) {
				throw RuntimeException("PDL respons inneholder ikke data")
			}

			return toPdlBruker(gqlResponse.data)
		}
	}

	fun hentIdenter(ident: String): List<Personident> {
		val requestBody = toJsonString(
			GraphqlUtils.GraphqlQuery(
				PdlQueries.HentIdenter.query,
				PdlQueries.HentIdenter.Variables(ident)
			)
		)

		val request = createGraphqlRequest(requestBody)

		httpClient.newCall(request).execute().use { response ->
			if (!response.isSuccessful) {
				throw RuntimeException("Klarte ikke å hente informasjon fra PDL. Status: ${response.code}")
			}

			val body = response.body?.string() ?: throw RuntimeException("Body is missing from PDL request")

			val gqlResponse = fromJsonString<PdlQueries.HentIdenter.Response>(body)

			logPdlWarnings(gqlResponse.extensions?.warnings)

			if (gqlResponse.data == null) {
				throw RuntimeException("PDL respons inneholder ikke data")
			}

			return gqlResponse.data.hentIdenter.identer.map { Personident(it.ident, it.historisk, IdentType.valueOf(it.gruppe)) }
		}
	}

	fun hentTelefon(ident: String): String? {
		val requestBody = toJsonString(
			GraphqlUtils.GraphqlQuery(
				PdlQueries.HentTelefon.query,
				PdlQueries.HentTelefon.Variables(ident)
			)
		)

		val request = createGraphqlRequest(requestBody)

		httpClient.newCall(request).execute().use { response ->
			if (!response.isSuccessful) {
				throw RuntimeException("Klarte ikke å hente informasjon fra PDL. Status: ${response.code}")
			}

			val body = response.body?.string() ?: throw RuntimeException("Body is missing from PDL request")

			val gqlResponse = fromJsonString<PdlQueries.HentTelefon.Response>(body)

			throwPdlApiErrors(gqlResponse)
			logPdlWarnings(gqlResponse.extensions?.warnings)

			if (gqlResponse.data == null) {
				throw RuntimeException("PDL respons inneholder ikke data")
			}

			return getTelefonnummer(gqlResponse.data.hentPerson.telefonnummer)
		}
	}

	private fun createGraphqlRequest(jsonPayload: String): Request {
		return Request.Builder()
			.url("$baseUrl/graphql")
			.addHeader("Authorization", "Bearer ${tokenProvider.get()}")
			.addHeader("Tema", "GEN")
			.addHeader("behandlingsnummer", behandlingsnummer)
			.post(jsonPayload.toRequestBody(mediaTypeJson))
			.build()
	}

	private fun toPdlBruker(response: PdlQueries.HentPerson.ResponseData): PdlPerson {
		val navn = response.hentPerson.navn.firstOrNull() ?: throw RuntimeException("PDL person mangler navn")
		val telefonnummer = getTelefonnummer(response.hentPerson.telefonnummer)
		val diskresjonskode = getDiskresjonskode(response.hentPerson.adressebeskyttelse)

		return PdlPerson(
			fornavn = navn.fornavn,
			mellomnavn = navn.mellomnavn,
			etternavn = navn.etternavn,
			telefonnummer = telefonnummer,
			adressebeskyttelseGradering = diskresjonskode,
			identer = response.hentIdenter.identer.map { Personident(it.ident, it.historisk, IdentType.valueOf(it.gruppe)) }
		)

	}

	private fun getTelefonnummer(telefonnummere: List<PdlQueries.Telefonnummer>): String? {
		val prioritertNummer = telefonnummere.minByOrNull { it.prioritet } ?: return null

		return "${prioritertNummer.landskode} ${prioritertNummer.nummer}"
	}

	private fun getDiskresjonskode(adressebeskyttelse: List<PdlQueries.HentPerson.Adressebeskyttelse>): AdressebeskyttelseGradering? {
		return when(adressebeskyttelse.firstOrNull()?.gradering) {
			"STRENGT_FORTROLIG_UTLAND" -> AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND
			"STRENGT_FORTROLIG" -> AdressebeskyttelseGradering.STRENGT_FORTROLIG
			"FORTROLIG" -> AdressebeskyttelseGradering.FORTROLIG
			"UGRADERT" -> AdressebeskyttelseGradering.UGRADERT
			else -> null
		}
	}

	private fun throwPdlApiErrors(response: GraphqlResponse<*, PdlQueries.PdlErrorExtension>) {
		var melding = "Feilmeldinger i respons fra pdl:\n"
		if(response.data == null) melding = "$melding- data i respons er null \n"
		response.errors?.let { feilmeldinger ->
			melding += feilmeldinger.joinToString(separator = "") { "- ${it.message} (code: ${it.extensions?.code} details: ${it.extensions?.details})\n" }
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
					"details: ${it.details}\n"
			)
		}

		log.warn(stringBuilder.toString())
	}

}
