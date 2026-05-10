package no.nav.amt.person.service.clients

import no.nav.amt.person.service.navbruker.InnsatsgruppeV1
import no.nav.amt.person.service.navbruker.InnsatsgruppeV2
import no.nav.common.token_client.client.MachineToMachineTokenClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.toEntity

@Service
class VeilarbvedtaksstotteClient(
    @Value($$"${veilarbvedtaksstotte.url}") url: String,
    @Value($$"${veilarbvedtaksstotte.scope}") private val scope: String,
    private val machineToMachineTokenClient: MachineToMachineTokenClient,
    restClientBuilder: RestClient.Builder,
) {
    private val restClient: RestClient = restClientBuilder
        .baseUrl("$url/veilarbvedtaksstotte")
        .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        .defaultRequest {
            it.header(HttpHeaders.AUTHORIZATION, "Bearer ${machineToMachineTokenClient.createMachineToMachineToken(scope)}")
        }.build()

    fun hentInnsatsgruppe(fnr: String): InnsatsgruppeV1? = runCatching {
        restClient
            .post()
            .uri("/api/hent-gjeldende-14a-vedtak")
            .body(PersonRequest(fnr))
            .retrieve()
            .toEntity<Gjeldende14aVedtakResponse>()
            .body
            ?.innsatsgruppe
            ?.toV1()
    }.getOrElse { e ->
        throw RuntimeException("Uventet status fra veilarbvedtaksstotte: ${e.message}", e)
    }

    private data class PersonRequest(
        val fnr: String,
    )

    internal data class Gjeldende14aVedtakResponse(
        val innsatsgruppe: InnsatsgruppeV2,
    )
}
