package no.nav.amt.person.service.clients.norg

import no.nav.common.rest.client.RestClient.baseClient
import okhttp3.OkHttpClient
import okhttp3.Request
import org.springframework.http.HttpStatus
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue

class NorgClient(
    private val url: String,
    private val objectMapper: ObjectMapper,
    private val httpClient: OkHttpClient = baseClient(),
) {
    private val enhetIdPattern = Regex("^\\d{4}$")

    private fun validateEnhetId(enhetId: String): String {
        require(enhetIdPattern.matches(enhetId)) {
            "Ugyldig enhetId-format"
        }
        return enhetId
    }

    private fun validateEnhetIds(enheter: List<String>): List<String> = enheter.map { validateEnhetId(it) }

    fun hentNavEnhet(enhetId: String): NorgNavEnhetDto? {
        val validatedEnhetId = validateEnhetId(enhetId)
        val request =
            Request
                .Builder()
                .url("$url/norg2/api/v1/enhet/$validatedEnhetId")
                .get()
                .build()

        httpClient.newCall(request).execute().use { response ->
            if (response.code == HttpStatus.NOT_FOUND.value()) {
                return null
            }

            if (!response.isSuccessful) {
                throw RuntimeException("Klarte ikke å hente enhet enhetId=$enhetId fra norg status=${response.code}")
            }

            val body = response.body.string()

            return objectMapper.readValue<NorgNavEnhetDto>(body)
        }
    }

    fun hentNavEnheter(enheter: List<String>): List<NorgNavEnhetDto> {
        val validatedEnheter = validateEnhetIds(enheter)
        val request =
            Request
                .Builder()
                .url("$url/norg2/api/v1/enhet?enhetsnummerListe=${validatedEnheter.joinToString(",")}")
                .get()
                .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw RuntimeException("Klarte ikke å hente enheter fra norg status=${response.code}")
            }

            val body = response.body.string()

            return objectMapper.readValue<List<NorgNavEnhetDto>>(body)
        }
    }
}
