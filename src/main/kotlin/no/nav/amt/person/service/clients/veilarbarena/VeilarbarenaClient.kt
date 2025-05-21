package no.nav.amt.person.service.clients.veilarbarena

import no.nav.amt.person.service.config.TeamLogs
import no.nav.amt.person.service.utils.JsonUtils.fromJsonString
import no.nav.amt.person.service.utils.JsonUtils.toJsonString
import no.nav.common.rest.client.RestClient.baseClient
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory
import java.util.function.Supplier

class VeilarbarenaClient(
	private val baseUrl: String,
	private val tokenProvider: Supplier<String>,
	private val httpClient: OkHttpClient = baseClient(),
	private val consumerId: String = "amt-person-service",
) {
	companion object {
		private val mediaTypeJson = "application/json".toMediaType()
	}
	private val log = LoggerFactory.getLogger(javaClass)

	fun hentBrukerOppfolgingsenhetId(fnr: String): String? {
		val personRequestJson = toJsonString(PersonRequest(fnr))
		val request = Request.Builder()
			.url("$baseUrl/veilarbarena/api/v2/arena/hent-status")
			.addHeader("Authorization", "Bearer ${tokenProvider.get()}")
			.addHeader("Nav-Consumer-Id", consumerId)
			.post(personRequestJson.toRequestBody(mediaTypeJson))
			.build()

		httpClient.newCall(request).execute().use { response ->
			if (response.code == 404) {
				TeamLogs.warn("Fant ikke bruker med fnr=$fnr i veilarbarena")
				log.warn("Kunne ikke hente oppfølgingsenhet, fant ikke bruker i veilarbarena")
				return null
			}

			if (!response.isSuccessful) {
				throw RuntimeException("Klarte ikke å hente status fra veilarbarena. Status: ${response.code}")
			}

			val body = response.body?.string() ?: throw RuntimeException("Body is missing")

			val statusDto = fromJsonString<BrukerArenaStatusDto>(body)

			return statusDto.oppfolgingsenhet
		}
	}

	private data class BrukerArenaStatusDto(
		var oppfolgingsenhet: String?
	)

	private data class PersonRequest(
		val fnr: String
	)
}
