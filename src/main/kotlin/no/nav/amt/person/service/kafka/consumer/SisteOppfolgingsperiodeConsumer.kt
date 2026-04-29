package no.nav.amt.person.service.kafka.consumer

import no.nav.amt.person.service.kafka.consumer.dto.SisteOppfolgingsperiodePayload
import no.nav.amt.person.service.navbruker.NavBrukerRepository
import no.nav.amt.person.service.navbruker.NavBrukerService
import no.nav.amt.person.service.navenhet.NavEnhetService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue

@Component
class SisteOppfolgingsperiodeConsumer(
    private val navBrukerRepository: NavBrukerRepository,
    private val navBrukerService: NavBrukerService,
    private val navEnhetService: NavEnhetService,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun ingest(value: String) {
        val sisteOppfolgingsperiodePayload = objectMapper.readValue<SisteOppfolgingsperiodePayload>(value)

        val navBruker = navBrukerRepository.get(sisteOppfolgingsperiodePayload.ident) ?: return

        if (navBruker.navEnhet?.enhetId == sisteOppfolgingsperiodePayload.kontor.kontorId) return

        log.info("Endrer oppfolgingsenhet på NavBruker med id=${navBruker.id}")

        val navEnhet = navEnhetService.hentEllerOpprettNavEnhet(sisteOppfolgingsperiodePayload.kontor.kontorId)

        navBrukerService.upsert(navBruker.copy(navEnhet = navEnhet))
    }
}
