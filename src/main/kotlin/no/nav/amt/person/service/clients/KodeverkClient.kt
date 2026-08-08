package no.nav.amt.person.service.clients

import no.nav.amt.person.service.poststed.Postnummer
import org.slf4j.LoggerFactory
import org.springframework.resilience.annotation.Retryable
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class KodeverkClient(
    private val api: KodeverkApi,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Retryable
    fun hentKodeverk(): List<Postnummer> = runCatching {
        api
            .hentPostnummerBetydninger(
                ekskluderUgyldige = true,
                oppslagsdato = LocalDate.now().toString(),
                spraak = "nb",
            ).toPostnummerListe()
    }.getOrElse { e ->
        log.error("Noe gikk galt ved henting av postnummer fra kodeverk: ${e.message}", e)
        throw RuntimeException("Noe gikk galt ved henting av postnummer fra kodeverk", e)
    }

    private fun KodeverkApi.GetKodeverkKoderBetydningerResponse.toPostnummerListe(): List<Postnummer> = betydninger.map {
        Postnummer(
            postnummer = it.key,
            poststed = it.value
                .first()
                .beskrivelser["nb"]
                ?.term
                ?: throw RuntimeException("Kode ${it.key} mangler term"),
        )
    }
}
