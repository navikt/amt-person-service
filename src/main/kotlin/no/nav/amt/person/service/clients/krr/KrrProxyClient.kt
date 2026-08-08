package no.nav.amt.person.service.clients.krr

import no.nav.amt.person.service.config.TeamLogs
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClientResponseException

@Service
class KrrProxyClient(
    private val krrProxyApi: KrrProxyApi,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun hentKontaktinformasjon(personident: String): Result<Kontaktinformasjon> = hentKontaktinformasjon(
        personidenter = setOf(personident),
    ).mapCatching {
        it[personident] ?: throw NoSuchElementException("Klarte ikke hente kontaktinformasjon for person")
    }

    fun hentKontaktinformasjon(personidenter: Set<String>): Result<Map<String, Kontaktinformasjon>> {
        try {
            val responseDto = krrProxyApi.hentPersoner(KrrProxyApi.PostPersonerRequest(personidenter))

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
}
