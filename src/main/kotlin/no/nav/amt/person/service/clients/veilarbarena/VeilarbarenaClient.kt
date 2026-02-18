package no.nav.amt.person.service.clients.veilarbarena

import no.nav.amt.person.service.clients.HeaderConstants.NAV_CONSUMER_ID_HEADER
import no.nav.amt.person.service.config.TeamLogs
import no.nav.amt.person.service.utils.OkHttpClientUtils.mediaTypeJson
import no.nav.common.rest.client.RestClient.baseClient
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue

class VeilarbarenaClient(
	private val baseUrl: String,
	private val tokenProvider: () -> String,
	private val objectMapper: ObjectMapper,
	private val httpClient: OkHttpClient = baseClient(),
	private val consumerId: String = "amt-person-service",
) {
	private val log = LoggerFactory.getLogger(javaClass)

	fun hentBrukerOppfolgingsenhetId(fnr: String): String? {
		val personRequestJson = objectMapper.writeValueAsString(PersonRequest(fnr))
		val request =
			Request
				.Builder()
				.url("$baseUrl/veilarbarena/api/v2/arena/hent-status")
				.addHeader(HttpHeaders.AUTHORIZATION, "Bearer ${tokenProvider()}")
				.addHeader(NAV_CONSUMER_ID_HEADER, consumerId)
				.post(personRequestJson.toRequestBody(mediaTypeJson))
				.build()

		httpClient.newCall(request).execute().use { response ->
			if (response.code == HttpStatus.NOT_FOUND.value()) {
				TeamLogs.warn("Fant ikke bruker med fnr=$fnr i veilarbarena")
				log.warn("Kunne ikke hente oppfølgingsenhet, fant ikke bruker i veilarbarena")
				return null
			}

			if (!response.isSuccessful) {
				throw RuntimeException("Klarte ikke å hente status fra veilarbarena. Status: ${response.code}")
			}

			val statusDto = objectMapper.readValue<BrukerArenaStatusResponse>(response.body.string())

			return statusDto.oppfolgingsenhet
		}
	}

	private data class BrukerArenaStatusResponse(
		var oppfolgingsenhet: String?,
	)

	private data class PersonRequest(
		val fnr: String,
	)
}
