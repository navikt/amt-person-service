package no.nav.amt.person.service.poststed

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import no.nav.amt.person.service.clients.KodeverkClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.UUID

@Component
class PoststedJob(
    val kodeverkClient: KodeverkClient,
    val poststedRepository: PoststedRepository,
    @Value("\${app.poststed.run-on-startup:false}")
    private val runOnStartup: Boolean,
    @Value("\${app.poststed.initial-delay:PT0S}")
    private val initialDelayString: String,
    @Value("\${app.poststed.period:P1D}")
    private val periodString: String,
    @Value("\${app.poststed.grace-period:PT30M}")
    private val gracePeriodString: String,
) : InitializingBean {
    private val log = LoggerFactory.getLogger(javaClass)
    
    private val initialDelay by lazy { Duration.parse(initialDelayString) }
    private val period by lazy { Duration.parse(periodString) }
    private val gracePeriod by lazy { Duration.parse(gracePeriodString) }

    override fun afterPropertiesSet() {
        if (runOnStartup) {
            log.info("Running PoststedJob on startup")
            run()
        }
    }

    @Scheduled(fixedDelayString = "\${app.poststed.period:86400000}", initialDelayString = "\${app.poststed.initial-delay:0}")
    @SchedulerLock(name = "PoststedJob", lockAtMostFor = "30m")
    fun run() {
        val sporingsId = UUID.randomUUID()
        log.info("Oppdaterer database med postnummer og poststed, $sporingsId")
        val postnummerListe = kodeverkClient.hentKodeverk(sporingsId)
        poststedRepository.oppdaterPoststed(postnummerListe.toSet(), sporingsId)
        log.info("Ferdig med å oppdatere poststed i database, $sporingsId")
    }
}
