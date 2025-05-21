package no.nav.amt.person.service.config

import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
class TeamLogsCheck : ApplicationListener<ApplicationReadyEvent> {
	private val log = LoggerFactory.getLogger(javaClass)

	override fun onApplicationEvent(event: ApplicationReadyEvent ) {
		TeamLogs.info("Applikasjonen har startet.")
		TeamLogs.warn("Team logs maskerer ikke fnr 15519932412")
		SecureLog.secureLog.info("SecureLog skal fortsatt fungere 15519932412")
		log.info("Vanlig logg skal maskere fnr 15519932412")
	}
}
