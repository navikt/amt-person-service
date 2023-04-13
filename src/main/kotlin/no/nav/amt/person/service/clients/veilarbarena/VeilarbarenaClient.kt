package no.nav.amt.person.service.clients.veilarbarena

import no.nav.amt.person.service.config.SecureLog.secureLog
import no.nav.amt.person.service.utils.JsonUtils.fromJsonString
import no.nav.common.rest.client.RestClient.baseClient
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.function.Supplier

class VeilarbarenaClient(
	private val baseUrl: String,
	private val tokenProvider: Supplier<String>,
	private val httpClient: OkHttpClient = baseClient(),
	private val consumerId: String = "amt-person-service",
) {

	fun hentBrukerOppfolgingsenhetId(fnr: String): String? {
		val request = Request.Builder()
			.url("$baseUrl/veilarbarena/api/arena/status?fnr=$fnr")
			.addHeader("Authorization", "Bearer ${tokenProvider.get()}")
			.addHeader("Nav-Consumer-Id", consumerId)
			.get()
			.build()

		httpClient.newCall(request).execute().use { response ->
			if (response.code == 404) {
				secureLog.warn("Fant ikke bruker med fnr=$fnr i veilarbarena")
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

}
