package no.nav.amt.person.service.clients

import org.springframework.security.oauth2.client.annotation.ClientRegistrationId
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.HttpExchange

@HttpExchange
@ClientRegistrationId(KODEVERK_API_CLIENT_ID)
interface KodeverkApi {
    @GetExchange("/api/v1/kodeverk/Postnummer/koder/betydninger")
    fun hentPostnummerBetydninger(
        @RequestParam ekskluderUgyldige: Boolean,
        @RequestParam oppslagsdato: String,
        @RequestParam spraak: String,
    ): GetKodeverkKoderBetydningerResponse

    data class GetKodeverkKoderBetydningerResponse(
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
