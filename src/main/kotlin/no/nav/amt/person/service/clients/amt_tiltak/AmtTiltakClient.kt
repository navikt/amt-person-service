package no.nav.amt.person.service.clients.amt_tiltak

import no.nav.amt.person.service.person.model.IdentType
import no.nav.amt.person.service.utils.JsonUtils.fromJsonString
import no.nav.common.rest.client.RestClient
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.UUID
import java.util.function.Supplier

class AmtTiltakClient(
	private val baseUrl: String,
	private val tokenProvider: Supplier<String>,
	private val httpClient: OkHttpClient = RestClient.baseClient(),
) {

	fun hentBrukerInfo(deltakerId: UUID): BrukerInfoDto {
		val request = Request.Builder()
			.url("$baseUrl/api/tiltaksarrangor/deltaker/$deltakerId/bruker-info")
			.header("Authorization", "Bearer ${tokenProvider.get()}")
			.get()
			.build()

		httpClient.newCall(request).execute().use {response ->
			if (!response.isSuccessful) {
				throw RuntimeException("Klarte ikke å hente brukerId for deltaker=$deltakerId fra amt-tiltak status=${response.code}")
			}

			val body = response.body?.string() ?: throw RuntimeException("Body is missing")

			return fromJsonString(body)
		}

	}

}

data class BrukerInfoDto(
	val brukerId: UUID,
	val navEnhetId: UUID?,
	val personIdentType: IdentType?,
	val historiskeIdenter: List<String>,
)
