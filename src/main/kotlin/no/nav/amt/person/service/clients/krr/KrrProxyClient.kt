package no.nav.amt.person.service.clients.krr

import no.nav.amt.person.service.config.SecureLog.secureLog
import no.nav.amt.person.service.utils.JsonUtils.fromJsonString
import no.nav.amt.person.service.utils.JsonUtils.toJsonString
import no.nav.common.rest.client.RestClient.baseClient
import no.nav.common.rest.client.RestClient.baseClientBuilder
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import java.util.concurrent.TimeUnit
import java.util.function.Supplier

class KrrProxyClient(
	private val baseUrl: String,
	private val tokenProvider: Supplier<String>,
	private val httpClient: OkHttpClient = baseClient(),
) {

	companion object {
		private val mediaTypeJson = "application/json".toMediaType()
		private const val INCREASED_TIMEOUT_SECONDS = 20L
	}

	fun hentKontaktinformasjon(personident: String): Result<Kontaktinformasjon> {
		val request: Request = Request.Builder()
			.url("$baseUrl/rest/v1/person?inkluderSikkerDigitalPost=false")
			.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenProvider.get())
			.header("Nav-Personident", personident)
			.build()

		httpClient.newCall(request).execute().use { response ->
			if (!response.isSuccessful) {
				return Result.failure(RuntimeException("Klarte ikke å hente kontaktinformasjon fra KRR-proxy. Status: ${response.code}"))
			}

			val body = response.body?.string() ?: return Result.failure(RuntimeException("Body manglet i respons fra KRR-proxy"))

			val responseDto = fromJsonString<KontaktinformasjonDto>(body)

			return Result.success(Kontaktinformasjon(
				epost = responseDto.epostadresse,
				telefonnummer = responseDto.mobiltelefonnummer,
			))
		}
	}


	fun hentKontaktinformasjon(personidenter: Set<String>): Result<KontaktinformasjonForPersoner> {
		val httpClientIncreasedTimeout = baseClientBuilder()
			.connectTimeout(INCREASED_TIMEOUT_SECONDS, TimeUnit.SECONDS)
			.readTimeout(INCREASED_TIMEOUT_SECONDS, TimeUnit.SECONDS)
			.writeTimeout(INCREASED_TIMEOUT_SECONDS, TimeUnit.SECONDS)
			.build()
		val requestBody = toJsonString(PostPersonerRequest(personidenter))

		val request: Request = Request.Builder()
			.url("$baseUrl/rest/v1/personer?inkluderSikkerDigitalPost=false")
			.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenProvider.get())
			.post(requestBody.toRequestBody(mediaTypeJson))
			.build()

		httpClientIncreasedTimeout.newCall(request).execute().use { response ->
			if (!response.isSuccessful) {
				return Result.failure(RuntimeException("Klarte ikke å hente kontaktinformasjon fra KRR-proxy. Status: ${response.code}"))
			}

			val body = response.body?.string() ?: return Result.failure(RuntimeException("Body manglet i respons fra KRR-proxy"))

			val responseDto = fromJsonString<PostPersonerResponse>(body)

			if(responseDto.feil.isNotEmpty()) {
				secureLog.error(responseDto.feil.toString())
				return Result.failure(RuntimeException("Respons fra KRR inneholdt feil på ${responseDto.feil.size} av ${personidenter.size} personer"))
			}
			return Result.success(KontaktinformasjonForPersoner(responseDto.personer))
		}
	}

	private data class PostPersonerRequest(
		val personidenter: Set<String>
	)

	private data class PostPersonerResponse(
		val personer: Map<String, Kontaktinformasjon>,
		val feil: Map<String, String>
	)

	private data class KontaktinformasjonDto(
		val personident: String,
		val epostadresse: String?,
		val mobiltelefonnummer: String?,
	)
}
