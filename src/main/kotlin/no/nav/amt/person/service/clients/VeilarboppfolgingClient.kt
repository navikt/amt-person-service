package no.nav.amt.person.service.clients

import no.nav.amt.person.service.navbruker.Oppfolgingsperiode
import no.nav.amt.person.service.utils.toSystemZoneLocalDateTime
import no.nav.common.token_client.client.MachineToMachineTokenClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.body
import org.springframework.web.client.toEntity
import java.time.ZonedDateTime
import java.util.UUID

@Service
class VeilarboppfolgingClient(
    @Value($$"${veilarboppfolging.url}") url: String,
    @Value($$"${veilarboppfolging.scope}") private val scope: String,
    private val machineToMachineTokenClient: MachineToMachineTokenClient,
    restClientBuilder: RestClient.Builder,
) {
    private val restClient: RestClient = restClientBuilder
        .baseUrl("$url/veilarboppfolging")
        .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        .defaultHeader(HeaderConstants.NAV_CONSUMER_ID_HEADER, "amt-person-service")
        .defaultRequest {
            it.header(HttpHeaders.AUTHORIZATION, "Bearer ${machineToMachineTokenClient.createMachineToMachineToken(scope)}")
        }.build()

    fun hentVeilederIdent(fnr: String): String? {
        try {
            val response = restClient
                .post()
                .uri("/api/v3/hent-veileder")
                .body(PersonRequest(fnr))
                .retrieve()
                .toEntity<HentBrukersVeilederResponse>()

            if (response.statusCode.value() == HttpStatus.NO_CONTENT.value()) return null
            return response.body?.veilederIdent
        } catch (e: RestClientResponseException) {
            throw RuntimeException("Uventet status ved kall mot veilarboppfolging ${e.statusCode.value()}", e)
        }
    }

    fun hentOppfolgingperioder(fnr: String): List<Oppfolgingsperiode> {
        try {
            return restClient
                .post()
                .uri("/api/v3/oppfolging/hent-perioder")
                .body(PersonRequest(fnr))
                .retrieve()
                .body<List<OppfolgingPeriodeDto>>()
                ?.map { it.toOppfolgingsperiode() }
                ?: emptyList()
        } catch (e: RestClientResponseException) {
            throw RuntimeException("Uventet status ved hent status-kall mot veilarboppfolging ${e.statusCode.value()}", e)
        }
    }

    private data class HentBrukersVeilederResponse(
        val veilederIdent: String,
    )

    private data class PersonRequest(
        val fnr: String,
    )

    internal data class OppfolgingPeriodeDto(
        val uuid: UUID,
        val startDato: ZonedDateTime,
        val sluttDato: ZonedDateTime?,
    ) {
        fun toOppfolgingsperiode() = Oppfolgingsperiode(
            id = uuid,
            startdato = startDato.toSystemZoneLocalDateTime(),
            sluttdato = sluttDato?.toSystemZoneLocalDateTime(),
        )
    }
}
