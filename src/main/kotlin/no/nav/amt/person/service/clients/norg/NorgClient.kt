package no.nav.amt.person.service.clients.norg

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.body

@Service
class NorgClient(
    @Value($$"${norg.url}") url: String,
    restClientBuilder: RestClient.Builder,
) {
    private val enhetIdPattern = Regex("^\\d{4}$")

    private val restClient: RestClient

    init {
        require(url.startsWith("https://") || url.startsWith("http://")) { "Ugyldig url-skjema for norg-klient" }
        restClient = restClientBuilder.baseUrl(url).build()
    }

    private fun validateEnhetId(enhetId: String): String {
        require(enhetIdPattern.matches(enhetId)) { "Ugyldig enhetId-format" }
        return enhetId
    }

    fun hentNavEnhet(enhetId: String): NorgNavEnhetDto? {
        val validatedEnhetId = validateEnhetId(enhetId)
        try {
            return restClient
                .get()
                .uri("/norg2/api/v1/enhet/{enhetId}", validatedEnhetId)
                .retrieve()
                .body<NorgNavEnhetDto>()
        } catch (e: RestClientResponseException) {
            if (e.statusCode.value() == 404) return null
            throw RuntimeException("Klarte ikke å hente enhet enhetId=$enhetId fra norg status=${e.statusCode.value()}", e)
        }
    }

    fun hentNavEnheter(enheter: List<String>): List<NorgNavEnhetDto> {
        val validatedEnheter = enheter.map { validateEnhetId(it) }
        try {
            return restClient
                .get()
                .uri { uriBuilder ->
                    uriBuilder
                        .path("/norg2/api/v1/enhet")
                        .queryParam("enhetsnummerListe", validatedEnheter.joinToString(","))
                        .build()
                }.retrieve()
                .body<List<NorgNavEnhetDto>>() ?: emptyList()
        } catch (e: RestClientResponseException) {
            throw RuntimeException("Klarte ikke å hente enheter fra norg status=${e.statusCode.value()}", e)
        }
    }
}
