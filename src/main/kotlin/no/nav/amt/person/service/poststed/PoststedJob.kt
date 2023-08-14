package no.nav.amt.person.service.poststed

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import no.nav.amt.person.service.clients.kodeverk.KodeverkClient
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class PoststedJob(
	val kodeverkClient: KodeverkClient,
	val poststedRepository: PoststedRepository
) {
	private val log = LoggerFactory.getLogger(javaClass)

	@Scheduled(cron = "0 0 6 * * *")
	@SchedulerLock(name = "PoststedJob", lockAtMostFor = "30m")
	fun run() {
		val sporingsId = UUID.randomUUID()
		log.info("Oppdaterer database med postnummer og poststed, $sporingsId")
		val postInformasjonListe = kodeverkClient.hentKodeverk(sporingsId)
		poststedRepository.oppdaterPoststed(postInformasjonListe, sporingsId)
		log.info("Ferdig med å oppdatere poststed i database, $sporingsId")
	}
}
