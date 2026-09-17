package no.nav.amt.person.service.kafka.consumer

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import no.nav.amt.person.service.clients.pdl.PdlClient
import no.nav.amt.person.service.navbruker.InnsatsgruppeV1
import no.nav.amt.person.service.navbruker.NavBrukerRepository
import no.nav.amt.person.service.navbruker.NavBrukerService
import no.nav.amt.person.service.person.model.Personident
import no.nav.amt.person.service.person.model.Personident.Companion.finnGjeldendeIdent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue
import java.time.Duration

@Component
class InnsatsgruppeConsumer(
    private val pdlClient: PdlClient,
    private val navBrukerRepository: NavBrukerRepository,
    private val navBrukerService: NavBrukerService,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    // Cacher gjeldende ident per aktørId for å unngå unødvendige oppslag mot PDL
    // for aktører vi nylig har slått opp. "Fant ikke person"-tilfeller caches ikke,
    // siden get(key, mappingFunction) ikke lagrer noe dersom mappingFunction kaster.
    private val gjeldendeIdentCache: Cache<String, Personident> = Caffeine
        .newBuilder()
        .maximumSize(10_000)
        .expireAfterWrite(Duration.ofHours(6))
        .build()

    fun ingest(value: String) {
        val siste14aVedtak = objectMapper.readValue<Siste14aVedtak>(value)

        val gjeldendeIdent = try {
            gjeldendeIdentCache.get(siste14aVedtak.aktorId) { aktorId ->
                pdlClient
                    .hentIdenter(aktorId)
                    .finnGjeldendeIdent()
                    .getOrThrow()
            }
        } catch (e: RuntimeException) {
            if (e.message?.contains("Fant ikke person") == true) {
                log.warn("Fant ikke person i PDL, hopper over Kafka-melding")
                return
            }
            throw e
        }

        val brukerId = navBrukerRepository.finnBrukerId(gjeldendeIdent.ident)

        if (brukerId == null) {
            log.info("Innsatsgruppe endret. Nav-bruker finnes ikke, hopper over Kafka-melding")
            return
        }

        navBrukerService.oppdaterInnsatsgruppe(
            navBrukerId = brukerId,
            innsatsgruppe = siste14aVedtak.innsatsgruppe,
        )
        log.info("Oppdatert innsatsgruppe for bruker $brukerId")
    }

    data class Siste14aVedtak(
        val aktorId: String,
        val innsatsgruppe: InnsatsgruppeV1,
    )
}
