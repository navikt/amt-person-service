package no.nav.amt.person.service.clients.veilarboppfolging

import no.nav.amt.person.service.utils.JsonUtils.fromJsonString
import no.nav.common.rest.client.RestClient.baseClient
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.function.Supplier

class VeilarboppfolgingClient(
	private val apiUrl: String,
	private val veilarboppfolgingTokenProvider: Supplier<String>,
	private val httpClient: OkHttpClient = baseClient(),
) {

	fun hentVeilederIdent(fnr: String) : String? {
		val request = Request.Builder()
			.url("$apiUrl/api/v2/veileder?fnr=$fnr")
			.header("Accept", "application/json; charset=utf-8")
			.header("Authorization", "Bearer ${veilarboppfolgingTokenProvider.get()}")
			.get()
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
}
