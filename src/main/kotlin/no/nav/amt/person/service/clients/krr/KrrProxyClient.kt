package no.nav.amt.person.service.clients.krr

import no.nav.amt.person.service.clients.HeaderConstants
import no.nav.amt.person.service.config.TeamLogs
import no.nav.common.token_client.client.MachineToMachineTokenClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.body

@Service
class KrrProxyClient(
    @Value($$"${digdir-krr-proxy.url}") baseUrl: String,
    @Value($$"${digdir-krr-proxy.scope}") private val scope: String,
    private val machineToMachineTokenClient: MachineToMachineTokenClient,
    restClientBuilder: RestClient.Builder,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val restClient: RestClient = restClientBuilder
        .baseUrl(baseUrl)
        .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        .defaultHeader(HeaderConstants.NAV_CONSUMER_ID_HEADER, "amt-person-service")
        .defaultRequest {
            it.header(HttpHeaders.AUTHORIZATION, "Bearer ${machineToMachineTokenClient.createMachineToMachineToken(scope)}")
        }.build()

    fun hentKontaktinformasjon(personident: String): Result<Kontaktinformasjon> = hentKontaktinformasjon(
        personidenter = setOf(personident),
    ).mapCatching {
        it[personident] ?: throw NoSuchElementException("Klarte ikke hente kontaktinformasjon for person")
    }

    fun hentKontaktinformasjon(personidenter: Set<String>): Result<Map<String, Kontaktinformasjon>> {
        try {
            val responseDto = restClient
                .post()
                .uri { uriBuilder ->
                    uriBuilder
                        .path("/rest/v1/personer")
                        .queryParam("inkluderSikkerDigitalPost", false)
                        .build()
                }.body(PostPersonerRequest(personidenter))
                .retrieve()
                .body<PostPersonerResponse>()
                ?: return Result.failure(RuntimeException("Tomt svar fra KRR-proxy"))

            if (responseDto.feil.isNotEmpty()) {
                TeamLogs.error(responseDto.feil.toString())
                log.warn("Respons fra KRR inneholdt feil på ${responseDto.feil.size} av ${personidenter.size} personer")
            }

            log.info("Hentet kontaktinformasjon for ${responseDto.personer.size} av ${personidenter.size} personer fra KRR-proxy")

            return Result.success(
                responseDto.personer.mapValues { (_, v) -> Kontaktinformasjon(v.epostadresse, v.mobiltelefonnummer) },
            )
        } catch (e: RestClientResponseException) {
            return Result.failure(
                RuntimeException("Klarte ikke å hente kontaktinformasjon fra KRR-proxy. Status: ${e.statusCode.value()}", e),
            )
        }
    }

    private data class PostPersonerRequest(
        val personidenter: Set<String>,
    )

    internal data class PostPersonerResponse(
        val personer: Map<String, KontaktinformasjonDto>,
        val feil: Map<String, String>,
    ) {
        data class KontaktinformasjonDto(
            val personident: String,
            val epostadresse: String?,
            val mobiltelefonnummer: String?,
        )
    }
}
