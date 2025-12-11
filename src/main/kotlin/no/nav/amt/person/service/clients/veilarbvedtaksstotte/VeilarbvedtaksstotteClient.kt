package no.nav.amt.person.service.clients.veilarbvedtaksstotte

import no.nav.amt.person.service.navbruker.InnsatsgruppeV1
import no.nav.amt.person.service.navbruker.InnsatsgruppeV2
import no.nav.amt.person.service.navbruker.InnsatsgruppeV2.Companion.toV1
import no.nav.amt.person.service.utils.OkHttpClientUtils.mediaTypeJson
import no.nav.common.rest.client.RestClient.baseClient
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue
import java.util.function.Supplier

class VeilarbvedtaksstotteClient(
	private val apiUrl: String,
	private val veilarbvedtaksstotteTokenProvider: Supplier<String>,
	private val objectMapper: ObjectMapper,
	private val httpClient: OkHttpClient = baseClient(),
) {
	fun hentInnsatsgruppe(fnr: String): InnsatsgruppeV1? {
		val personRequestJson = objectMapper.writeValueAsString(PersonRequest(fnr))
		val request =
			Request
				.Builder()
				.url("$apiUrl/api/hent-gjeldende-14a-vedtak")
				.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
				.header(HttpHeaders.AUTHORIZATION, "Bearer ${veilarbvedtaksstotteTokenProvider.get()}")
				.post(personRequestJson.toRequestBody(mediaTypeJson))
				.build()

		httpClient.newCall(request).execute().use { response ->
			if (!response.isSuccessful) {
				throw RuntimeException("Uventet status fra veilarbvedtaksstotte ${response.code}")
			}
			val body = response.body.string()

			if (body.isEmpty()) {
				return null
			}

			val gjeldende14aVedtakRespons = objectMapper.readValue<Gjeldende14aVedtakDTO>(body)

			return gjeldende14aVedtakRespons.innsatsgruppe.toV1()
		}
	}

	private data class PersonRequest(
		val fnr: String,
	)

	data class Gjeldende14aVedtakDTO(
		val innsatsgruppe: InnsatsgruppeV2,
	)
}
