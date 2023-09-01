package no.nav.amt.person.service.nav_bruker

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import no.nav.common.job.JobRunner
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class NavBrukerUpdateJob(
	private val navBrukerService: NavBrukerService
) {

	@Scheduled(cron = "@midnight")
	@SchedulerLock(name = "navBrukerUpdater", lockAtMostFor = "60m")
	fun update() {
		JobRunner.run("oppdater_nav_brukere") { oppdaterBrukere() }
	}

	private fun oppdaterBrukere() {
		val personidenter = navBrukerService.getPersonidenter(offset = 0, limit = 10000, notSyncedSince = LocalDateTime.now().minusDays(3))
		navBrukerService.syncKontaktinfoBulk(personidenter)
	}
}
