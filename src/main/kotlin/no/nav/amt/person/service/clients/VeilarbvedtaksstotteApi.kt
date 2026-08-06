package no.nav.amt.person.service.clients

import no.nav.amt.person.service.navbruker.InnsatsgruppeV2
import org.springframework.security.oauth2.client.annotation.ClientRegistrationId
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.service.annotation.HttpExchange
import org.springframework.web.service.annotation.PostExchange

@HttpExchange("/veilarbvedtaksstotte")
@ClientRegistrationId(VEILARBVEDTAKSSTOTTE_CLIENT_ID)
interface VeilarbvedtaksstotteApi {
    @PostExchange("/api/hent-gjeldende-14a-vedtak")
    fun hentGjeldende14aVedtak(
        @RequestBody request: PersonRequest,
    ): Gjeldende14aVedtakResponse?

    data class PersonRequest(
        val fnr: String,
    )

    data class Gjeldende14aVedtakResponse(
        val innsatsgruppe: InnsatsgruppeV2,
    )
}
