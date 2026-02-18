package no.nav.amt.person.service.clients.norg

import no.nav.common.rest.client.RestClient.baseClient
import okhttp3.OkHttpClient
import okhttp3.Request
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue

class NorgClient(
	private val url: String,
	private val objectMapper: ObjectMapper,
	private val httpClient: OkHttpClient = baseClient(),
) {
	fun hentNavEnhet(enhetId: String): NorgNavEnhetDto? {
		val request =
			Request
				.Builder()
				.url("$url/norg2/api/v1/enhet/$enhetId")
				.get()
				.build()

		httpClient.newCall(request).execute().use { response ->
			if (response.code == 404) {
				return null
			}

			if (!response.isSuccessful) {
				throw RuntimeException("Klarte ikke å hente enhet enhetId=$enhetId fra norg status=${response.code}")
			}

			val body = response.body.string()

			return objectMapper.readValue<NorgNavEnhetDto>(body)
		}
	}

	fun hentNavEnheter(enheter: List<String>): List<NorgNavEnhetDto> {
		val request =
			Request
				.Builder()
				.url("$url/norg2/api/v1/enhet?enhetsnummerListe=${enheter.joinToString(",")}")
				.get()
				.build()

		httpClient.newCall(request).execute().use { response ->
			if (!response.isSuccessful) {
				throw RuntimeException("Klarte ikke å hente enheter fra norg status=${response.code}")
			}

			val body = response.body.string()

			return objectMapper.readValue<List<NorgNavEnhetDto>>(body)
		}
	}
}
