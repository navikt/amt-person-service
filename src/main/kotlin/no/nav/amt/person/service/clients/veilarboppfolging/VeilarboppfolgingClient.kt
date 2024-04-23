package no.nav.amt.person.service.clients.veilarboppfolging

import no.nav.amt.person.service.nav_bruker.Oppfolgingsperiode
import no.nav.amt.person.service.utils.JsonUtils.fromJsonString
import no.nav.common.rest.client.RestClient.baseClient
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.function.Supplier
import no.nav.amt.person.service.utils.JsonUtils.toJsonString
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.ZonedDateTime
import java.util.UUID

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
			.url("$apiUrl/api/v3/hent-veileder")
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

	fun hentOppfolgingperioder(fnr: String) : List<Oppfolgingsperiode> {
		val personRequestJson = toJsonString(PersonRequest(fnr))
		val request = Request.Builder()
			.url("$apiUrl/api/v3/oppfolging/hent-status")
			.header("Accept", "application/json; charset=utf-8")
			.header("Authorization", "Bearer ${veilarboppfolgingTokenProvider.get()}")
			.post(personRequestJson.toRequestBody(mediaTypeJson))
			.build()

		httpClient.newCall(request).execute().use { response ->
			if (!response.isSuccessful) {
				throw RuntimeException("Uventet status ved hent status-kall mot veilarboppfolging ${response.code}")
			}
			val body = response.body?.string() ?: throw RuntimeException("Body mangler i hent status-respons fra veilarboppfolging")

			val oppfolgingStatusRespons = fromJsonString<OppfolgingStatus>(body)

			return oppfolgingStatusRespons.oppfolgingsPerioder.map { it.toOppfolgingsperiode() }
		}
	}

	data class HentBrukersVeilederResponse (
		val veilederIdent: String
	)

	private data class PersonRequest(
		val fnr: String
	)

	data class OppfolgingStatus(
		val oppfolgingsPerioder: List<OppfolgingPeriodeDTO>
	)

	data class OppfolgingPeriodeDTO(
		val uuid: UUID,
		val startDato: ZonedDateTime,
		val sluttDato: ZonedDateTime?
	) {
		fun toOppfolgingsperiode() = Oppfolgingsperiode(
			id = uuid,
			startdato = startDato.toLocalDateTime(),
			sluttdato = sluttDato?.toLocalDateTime()
		)
	}
}
