package no.nav.amt.person.service.clients.norg

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClientResponseException

@Service
class NorgClient(
    @Value($$"${spring.http.serviceclient.norg-api.base-url}") url: String,
    private val norgApi: NorgApi,
) {
    private val enhetIdPattern = Regex("^\\d{4}$")

    init {
        require(url.startsWith("https://") || url.startsWith("http://")) { "Ugyldig url-skjema for norg-klient" }
    }

    fun hentNavEnhet(enhetId: String): NorgNavEnhetDto? {
        val validatedEnhetId = validateEnhetId(enhetId)
        return try {
            norgApi.hentNavEnhet(validatedEnhetId)
        } catch (e: RestClientResponseException) {
            if (e.statusCode.value() == 404) return null

            throw RuntimeException(
                "Klarte ikke å hente enhetId=$enhetId fra norg status=${e.statusCode.value()}",
                e,
            )
        }
    }

    fun hentNavEnheter(enheter: List<String>): List<NorgNavEnhetDto> {
        val validatedEnheter = enheter.map { validateEnhetId(it) }

        return try {
            norgApi.hentNavEnheter(validatedEnheter.joinToString(","))
        } catch (e: RestClientResponseException) {
            throw RuntimeException("Klarte ikke å hente enheter fra norg status=${e.statusCode.value()}", e)
        }
    }

    private fun validateEnhetId(enhetId: String): String {
        require(enhetIdPattern.matches(enhetId)) { "Ugyldig enhetId-format" }
        return enhetId
    }
}
