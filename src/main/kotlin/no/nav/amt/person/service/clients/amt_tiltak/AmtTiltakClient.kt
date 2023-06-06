package no.nav.amt.person.service.clients.amt_tiltak

import no.nav.amt.person.service.person.model.IdentType
import no.nav.amt.person.service.utils.JsonUtils.fromJsonString
import no.nav.common.rest.client.RestClient
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
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
	fun hentBrukerId(personIdent: String): UUID? {
		val request = Request.Builder()
			.url("$baseUrl/api/tiltaksarrangor/deltaker/bruker-info")
			.header("Authorization", "Bearer ${tokenProvider.get()}")
			.post("""{"personident": "$personIdent"}""".toRequestBody("application/json".toMediaType()))
			.build()

		httpClient.newCall(request).execute().use {response ->
			if (response.code == 404) {
				return null
			}

			if (!response.isSuccessful) {
				throw RuntimeException("Klarte ikke å hente brukerId fra amt-tiltak status=${response.code}")
			}

			val body = response.body?.string() ?: throw RuntimeException("Body is missing")

			val brukerInfo =  fromJsonString<BrukerInfoDto>(body)

			return brukerInfo.brukerId
		}
	}

}

data class BrukerInfoDto(
	val brukerId: UUID,
	val navEnhetId: UUID?,
	val personIdentType: IdentType?,
	val historiskeIdenter: List<String>,
)
