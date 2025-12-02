package no.nav.amt.person.service.clients.veilarbvedtaksstotte

import no.nav.amt.person.service.navbruker.InnsatsgruppeV1
import no.nav.amt.person.service.navbruker.InnsatsgruppeV2
import no.nav.amt.person.service.navbruker.InnsatsgruppeV2.Companion.toV1
import no.nav.amt.person.service.utils.JsonUtils.fromJsonString
import no.nav.amt.person.service.utils.JsonUtils.toJsonString
import no.nav.common.rest.client.RestClient.baseClient
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.function.Supplier

class VeilarbvedtaksstotteClient(
	private val apiUrl: String,
	private val veilarbvedtaksstotteTokenProvider: Supplier<String>,
	private val httpClient: OkHttpClient = baseClient(),
) {
	companion object {
		private val mediaTypeJson = "application/json".toMediaType()
	}

	fun hentInnsatsgruppe(fnr: String): InnsatsgruppeV1? {
		val personRequestJson = toJsonString(PersonRequest(fnr))
		val request =
			Request
				.Builder()
				.url("$apiUrl/api/hent-gjeldende-14a-vedtak")
				.header("Accept", "application/json; charset=utf-8")
				.header("Authorization", "Bearer ${veilarbvedtaksstotteTokenProvider.get()}")
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

			val gjeldende14aVedtakRespons = fromJsonString<Gjeldende14aVedtakDTO>(body)

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
