package no.nav.amt.person.service.clients

import no.nav.amt.person.service.navbruker.InnsatsgruppeV1
import org.springframework.stereotype.Service

@Service
class VeilarbvedtaksstotteClient(
    private val api: VeilarbvedtaksstotteApi,
) {
    fun hentInnsatsgruppe(fnr: String): InnsatsgruppeV1? = runCatching {
        api
            .hentGjeldende14aVedtak(VeilarbvedtaksstotteApi.PersonRequest(fnr))
            ?.innsatsgruppe
            ?.toV1()
    }.getOrElse { e ->
        throw RuntimeException("Uventet status fra veilarbvedtaksstotte: ${e.message}", e)
    }
}
