package no.nav.amt.person.service.clients

import no.nav.amt.person.service.poststed.Postnummer
import no.nav.common.token_client.client.MachineToMachineTokenClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.resilience.annotation.Retryable
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import java.time.LocalDate
import java.util.UUID

@Service
class KodeverkClient(
    @Value($$"${kodeverk.url}") url: String,
    @Value($$"${kodeverk.scope}") private val scope: String,
    restClientBuilder: RestClient.Builder,
    private val machineToMachineTokenClient: MachineToMachineTokenClient,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val restClient: RestClient = restClientBuilder
        .baseUrl(url)
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
        .defaultHeader(HeaderConstants.NAV_CONSUMER_ID_HEADER, "amt-person-service")
        .defaultRequest { spec ->
            spec.header(HttpHeaders.AUTHORIZATION, "Bearer ${machineToMachineTokenClient.createMachineToMachineToken(scope)}")
        }.build()

    @Retryable
    fun hentKodeverk(callId: UUID): List<Postnummer> = runCatching {
        restClient
            .get()
            .uri { uriBuilder ->
                uriBuilder
                    .path("/api/v1/kodeverk/Postnummer/koder/betydninger")
                    .queryParam("ekskluderUgyldige", true)
                    .queryParam("oppslagsdato", LocalDate.now())
                    .queryParam("spraak", "nb")
                    .build()
            }.header(HeaderConstants.NAV_CALL_ID_HEADER, callId.toString())
            .retrieve()
            .body<GetKodeverkKoderBetydningerResponse>()
            ?: throw RuntimeException("Tomt svar fra kodeverk")
    }.map { it.toPostnummerListe() }
        .getOrElse { e ->
            log.error("Noe gikk galt ved henting av postnummer fra kodeverk: ${e.message}", e)
            throw RuntimeException("Noe gikk galt ved henting av postnummer fra kodeverk", e)
        }

    private fun GetKodeverkKoderBetydningerResponse.toPostnummerListe(): List<Postnummer> = betydninger.map {
        Postnummer(
            postnummer = it.key,
            poststed =
                it.value
                    .first()
                    .beskrivelser["nb"]
                    ?.term
                    ?: throw RuntimeException("Kode ${it.key} mangler term"),
        )
    }

    private data class GetKodeverkKoderBetydningerResponse(
        val betydninger: Map<String, List<Betydning>>,
    ) {
        data class Betydning(
            val beskrivelser: Map<String, Beskrivelse>,
        ) {
            data class Beskrivelse(
                val term: String,
            )
        }
    }
}
