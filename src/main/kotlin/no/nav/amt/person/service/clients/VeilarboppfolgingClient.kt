package no.nav.amt.person.service.clients

import no.nav.amt.person.service.navbruker.Oppfolgingsperiode
import no.nav.amt.person.service.utils.toSystemZoneLocalDateTime
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClientResponseException

@Service
class VeilarboppfolgingClient(
    private val api: VeilarboppfolgingApi,
) {
    fun hentVeilederIdent(fnr: String): String? {
        try {
            val response = api.hentVeileder(VeilarboppfolgingApi.PersonRequest(fnr))
            if (response.statusCode == HttpStatus.NO_CONTENT) return null
            return response.body?.veilederIdent
        } catch (e: RestClientResponseException) {
            throw RuntimeException("Uventet status ved kall mot veilarboppfolging ${e.statusCode.value()}", e)
        }
    }

    fun hentOppfolgingperioder(fnr: String): List<Oppfolgingsperiode> {
        try {
            return api
                .hentOppfolgingsperioder(VeilarboppfolgingApi.PersonRequest(fnr))
                .map {
                    Oppfolgingsperiode(
                        id = it.uuid,
                        startdato = it.startDato.toSystemZoneLocalDateTime(),
                        sluttdato = it.sluttDato?.toSystemZoneLocalDateTime(),
                    )
                }
        } catch (e: RestClientResponseException) {
            throw RuntimeException("Uventet status ved hent status-kall mot veilarboppfolging ${e.statusCode.value()}", e)
        }
    }
}
