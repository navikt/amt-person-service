package no.nav.amt.person.service.clients.veilarboppfolging

import no.nav.amt.person.service.utils.JsonUtils.fromJsonString
import no.nav.common.rest.client.RestClient.baseClient
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.function.Supplier
import no.nav.amt.person.service.utils.JsonUtils.toJsonString
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

class VeilarboppfolgingClient(
	private val apiUrl: String,
	private val veilarboppfolgingTokenProvider: Supplier<String>,
	private val httpClient: OkHttpClient = baseClient(),
) {
	companion object {
		private val mediaTypeJson = "application/json".toMediaType()
	}

	fun hentVeilederIdent(fnr: String) : String? {
		val personRequestJson = toJsonString(PersonRequest(fnr))
		val request = Request.Builder()
			.url("$apiUrl/api/v3//hent-veileder")
			.header("Accept", "application/json; charset=utf-8")
			.header("Authorization", "Bearer ${veilarboppfolgingTokenProvider.get()}")
			.post(personRequestJson.toRequestBody(mediaTypeJson))
			.build()

		httpClient.newCall(request).execute().use { response ->
			response.takeIf { !it.isSuccessful }
				?.let { throw RuntimeException("Uventet status ved kall mot veilarboppfolging ${it.code}") }

			response.takeIf { it.code == 204 }
				?.let { return null }

			response.takeIf { it.body == null }
				?.let { throw RuntimeException("Body mangler i respons fra veilarboppfolging") }

			val veilederRespons = fromJsonString<HentBrukersVeilederResponse>(response.body!!.string())

			return veilederRespons.veilederIdent
		}

	}

	data class HentBrukersVeilederResponse (
		val veilederIdent: String
	)

	private data class PersonRequest(
		val fnr: String
	)
}
