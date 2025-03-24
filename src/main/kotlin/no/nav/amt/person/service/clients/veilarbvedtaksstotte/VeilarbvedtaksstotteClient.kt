package no.nav.amt.person.service.clients.veilarbvedtaksstotte

import no.nav.amt.person.service.nav_bruker.Innsatsgruppe
import no.nav.amt.person.service.nav_bruker.InnsatsgruppeV1
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
		val request = Request.Builder()
			.url("$apiUrl/api/hent-gjeldende-14a-vedtak")
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

			val gjeldende14aVedtakRespons = fromJsonString<Gjeldende14aVedtakDTO>(body)

			return gjeldende14aVedtakRespons.innsatsgruppe.toV1()
		}
	}


	private data class PersonRequest(
		val fnr: String
	)

	data class Gjeldende14aVedtakDTO(
		val innsatsgruppe: Innsatsgruppe
	)
}

fun Innsatsgruppe.toV1(): InnsatsgruppeV1 {
	return when (this) {
		Innsatsgruppe.GODE_MULIGHETER -> InnsatsgruppeV1.STANDARD_INNSATS
		Innsatsgruppe.TRENGER_VEILEDNING -> InnsatsgruppeV1.SITUASJONSBESTEMT_INNSATS
		Innsatsgruppe.TRENGER_VEILEDNING_NEDSATT_ARBEIDSEVNE -> InnsatsgruppeV1.SPESIELT_TILPASSET_INNSATS
		Innsatsgruppe.JOBBE_DELVIS -> InnsatsgruppeV1.GRADERT_VARIG_TILPASSET_INNSATS
		Innsatsgruppe.LITEN_MULIGHET_TIL_A_JOBBE -> InnsatsgruppeV1.VARIG_TILPASSET_INNSATS
	}
}
