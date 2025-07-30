package no.nav.amt.person.service.clients.krr

import no.nav.amt.person.service.config.TeamLogs
import no.nav.amt.person.service.utils.JsonUtils.fromJsonString
import no.nav.amt.person.service.utils.JsonUtils.toJsonString
import no.nav.common.rest.client.RestClient.baseClientBuilder
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import java.util.concurrent.TimeUnit
import java.util.function.Supplier

class KrrProxyClient(
	private val baseUrl: String,
	private val tokenProvider: Supplier<String>,
) {
	private val httpClient =
		baseClientBuilder()
			.connectTimeout(INCREASED_TIMEOUT_SECONDS, TimeUnit.SECONDS)
			.readTimeout(INCREASED_TIMEOUT_SECONDS, TimeUnit.SECONDS)
			.writeTimeout(INCREASED_TIMEOUT_SECONDS, TimeUnit.SECONDS)
			.build()

	private val log = LoggerFactory.getLogger(javaClass)

	companion object {
		private val mediaTypeJson = "application/json".toMediaType()
		private const val INCREASED_TIMEOUT_SECONDS = 20L
	}

	fun hentKontaktinformasjon(personident: String) =
		hentKontaktinformasjon(setOf(personident)).mapCatching {
			it[personident] ?: throw NoSuchElementException("Klarte ikke hente kontaktinformasjon for person")
		}

	fun hentKontaktinformasjon(personidenter: Set<String>): Result<KontaktinformasjonForPersoner> {
		val requestBody = toJsonString(PostPersonerRequest(personidenter))

		val request: Request =
			Request
				.Builder()
				.url("$baseUrl/rest/v1/personer?inkluderSikkerDigitalPost=false")
				.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenProvider.get())
				.post(requestBody.toRequestBody(mediaTypeJson))
				.build()

		httpClient.newCall(request).execute().use { response ->
			if (!response.isSuccessful) {
				return Result.failure(RuntimeException("Klarte ikke å hente kontaktinformasjon fra KRR-proxy. Status: ${response.code}"))
			}

			val responseDto = fromJsonString<PostPersonerResponse>(response.body.string())

			if (responseDto.feil.isNotEmpty()) {
				TeamLogs.error(responseDto.feil.toString())
				log.warn("Respons fra KRR inneholdt feil på ${responseDto.feil.size} av ${personidenter.size} personer")
			}

			log.info("Hentet kontaktinformasjon for ${responseDto.personer.size} av ${personidenter.size} personer fra KRR-proxy")

			return Result.success(
				responseDto.personer.mapValues { (_, v) -> Kontaktinformasjon(v.epostadresse, v.mobiltelefonnummer) },
			)
		}
	}

	private data class PostPersonerRequest(
		val personidenter: Set<String>,
	)

	private data class PostPersonerResponse(
		val personer: Map<String, KontaktinformasjonDto>,
		val feil: Map<String, String>,
	)

	private data class KontaktinformasjonDto(
		val personident: String,
		val epostadresse: String?,
		val mobiltelefonnummer: String?,
	)
}
