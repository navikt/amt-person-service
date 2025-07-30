package no.nav.amt.person.service.clients.veilarboppfolging

import no.nav.amt.person.service.navbruker.Oppfolgingsperiode
import no.nav.amt.person.service.utils.JsonUtils.fromJsonString
import no.nav.amt.person.service.utils.JsonUtils.toJsonString
import no.nav.amt.person.service.utils.toSystemZoneLocalDateTime
import no.nav.common.rest.client.RestClient.baseClient
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import java.time.ZonedDateTime
import java.util.UUID
import java.util.function.Supplier

class VeilarboppfolgingClient(
	private val apiUrl: String,
	private val veilarboppfolgingTokenProvider: Supplier<String>,
	private val httpClient: OkHttpClient = baseClient(),
) {
	companion object {
		private val mediaTypeJson = "application/json".toMediaType()
	}

	fun hentVeilederIdent(fnr: String): String? {
		val personRequestJson = toJsonString(PersonRequest(fnr))
		val request =
			Request
				.Builder()
				.url("$apiUrl/api/v3/hent-veileder")
				.header("Accept", "application/json; charset=utf-8")
				.header("Authorization", "Bearer ${veilarboppfolgingTokenProvider.get()}")
				.post(personRequestJson.toRequestBody(mediaTypeJson))
				.build()

		httpClient.newCall(request).execute().use { response ->
			if (!response.isSuccessful) {
				throw RuntimeException("Uventet status ved kall mot veilarboppfolging ${response.code}")
			}

			if (response.code == HttpStatus.NO_CONTENT.value()) return null

			val veilederRespons = fromJsonString<HentBrukersVeilederResponse>(response.body.string())
			return veilederRespons.veilederIdent
		}
	}

	fun hentOppfolgingperioder(fnr: String): List<Oppfolgingsperiode> {
		val personRequestJson = toJsonString(PersonRequest(fnr))
		val request =
			Request
				.Builder()
				.url("$apiUrl/api/v3/oppfolging/hent-perioder")
				.header(HttpHeaders.ACCEPT, "application/json; charset=utf-8")
				.header(HttpHeaders.AUTHORIZATION, "Bearer ${veilarboppfolgingTokenProvider.get()}")
				.post(personRequestJson.toRequestBody(mediaTypeJson))
				.build()

		httpClient.newCall(request).execute().use { response ->
			if (!response.isSuccessful) {
				throw RuntimeException("Uventet status ved hent status-kall mot veilarboppfolging ${response.code}")
			}

			val oppfolgingsperioderRespons = fromJsonString<List<OppfolgingPeriodeDTO>>(response.body.string())
			return oppfolgingsperioderRespons.map { it.toOppfolgingsperiode() }
		}
	}

	data class HentBrukersVeilederResponse(
		val veilederIdent: String,
	)

	private data class PersonRequest(
		val fnr: String,
	)

	data class OppfolgingPeriodeDTO(
		val uuid: UUID,
		val startDato: ZonedDateTime,
		val sluttDato: ZonedDateTime?,
	) {
		fun toOppfolgingsperiode() =
			Oppfolgingsperiode(
				id = uuid,
				startdato = startDato.toSystemZoneLocalDateTime(),
				sluttdato = sluttDato?.toSystemZoneLocalDateTime(),
			)
	}
}
