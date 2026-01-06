package no.nav.amt.person.service.clients.veilarboppfolging

import no.nav.amt.person.service.navbruker.Oppfolgingsperiode
import no.nav.amt.person.service.utils.OkHttpClientUtils.mediaTypeJson
import no.nav.amt.person.service.utils.toSystemZoneLocalDateTime
import no.nav.common.rest.client.RestClient.baseClient
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue
import java.time.ZonedDateTime
import java.util.UUID
import java.util.function.Supplier

class VeilarboppfolgingClient(
	private val apiUrl: String,
	private val veilarboppfolgingTokenProvider: Supplier<String>,
	private val objectMapper: ObjectMapper,
	private val httpClient: OkHttpClient = baseClient(),
) {
	fun hentVeilederIdent(fnr: String): String? {
		val personRequestJson = objectMapper.writeValueAsString(PersonRequest(fnr))
		val request =
			Request
				.Builder()
				.url("$apiUrl/api/v3/hent-veileder")
				.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
				.header(HttpHeaders.AUTHORIZATION, "Bearer ${veilarboppfolgingTokenProvider.get()}")
				.post(personRequestJson.toRequestBody(mediaTypeJson))
				.build()

		httpClient.newCall(request).execute().use { response ->
			if (!response.isSuccessful) {
				throw RuntimeException("Uventet status ved kall mot veilarboppfolging ${response.code}")
			}

			if (response.code == HttpStatus.NO_CONTENT.value()) return null

			val veilederRespons = objectMapper.readValue<HentBrukersVeilederResponse>(response.body.string())
			return veilederRespons.veilederIdent
		}
	}

	fun hentOppfolgingperioder(fnr: String): List<Oppfolgingsperiode> {
		val personRequestJson = objectMapper.writeValueAsString(PersonRequest(fnr))
		val request =
			Request
				.Builder()
				.url("$apiUrl/api/v3/oppfolging/hent-perioder")
				.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
				.header(HttpHeaders.AUTHORIZATION, "Bearer ${veilarboppfolgingTokenProvider.get()}")
				.post(personRequestJson.toRequestBody(mediaTypeJson))
				.build()

		httpClient.newCall(request).execute().use { response ->
			if (!response.isSuccessful) {
				throw RuntimeException("Uventet status ved hent status-kall mot veilarboppfolging ${response.code}")
			}

			val oppfolgingsperioderRespons = objectMapper.readValue<List<OppfolgingPeriodeDTO>>(response.body.string())
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
