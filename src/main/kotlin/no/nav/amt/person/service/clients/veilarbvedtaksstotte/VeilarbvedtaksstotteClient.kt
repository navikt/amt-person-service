package no.nav.amt.person.service.clients.veilarbvedtaksstotte

import no.nav.amt.person.service.nav_bruker.Innsatsgruppe
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

	fun hentInnsatsgruppe(fnr: String): Innsatsgruppe? {
		val personRequestJson = toJsonString(PersonRequest(fnr))
		val request = Request.Builder()
			.url("$apiUrl/api/v2/hent-siste-14a-vedtak")
			.header("Accept", "application/json; charset=utf-8")
			.header("Authorization", "Bearer ${veilarbvedtaksstotteTokenProvider.get()}")
			.post(personRequestJson.toRequestBody(mediaTypeJson))
			.build()

		httpClient.newCall(request).execute().use { response ->
			if (!response.isSuccessful) {
				throw RuntimeException("Uventet status fra veilarbvedtaksstotte ${response.code}")
			}
			val body = response.body?.string()

			if (body.isNullOrEmpty()) {
				return null
			}

			val siste14aVedtakDTORespons = fromJsonString<Siste14aVedtakDTO>(body)

			return siste14aVedtakDTORespons.innsatsgruppe.toInnsatsgruppe()
		}
	}

	private data class PersonRequest(
		val fnr: String
	)

	data class Siste14aVedtakDTO(
		val innsatsgruppe: InnsatsgruppeDto
	)

	enum class InnsatsgruppeDto {
		STANDARD_INNSATS,
		SITUASJONSBESTEMT_INNSATS,
		SPESIELT_TILPASSET_INNSATS,
		GRADERT_VARIG_TILPASSET_INNSATS,
		VARIG_TILPASSET_INNSATS
	}
}

fun VeilarbvedtaksstotteClient.InnsatsgruppeDto.toInnsatsgruppe(): Innsatsgruppe {
	return when (this) {
		VeilarbvedtaksstotteClient.InnsatsgruppeDto.STANDARD_INNSATS -> Innsatsgruppe.STANDARD_INNSATS
		VeilarbvedtaksstotteClient.InnsatsgruppeDto.SITUASJONSBESTEMT_INNSATS -> Innsatsgruppe.SITUASJONSBESTEMT_INNSATS
		VeilarbvedtaksstotteClient.InnsatsgruppeDto.SPESIELT_TILPASSET_INNSATS -> Innsatsgruppe.SPESIELT_TILPASSET_INNSATS
		VeilarbvedtaksstotteClient.InnsatsgruppeDto.GRADERT_VARIG_TILPASSET_INNSATS -> Innsatsgruppe.VARIG_TILPASSET_INNSATS
		VeilarbvedtaksstotteClient.InnsatsgruppeDto.VARIG_TILPASSET_INNSATS -> Innsatsgruppe.VARIG_TILPASSET_INNSATS
	}
}
