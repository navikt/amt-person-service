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
		TeamLogs.warn("Meldingen her skal bare vises i teamlogs")
		SecureLog.secureLog.info("SecureLog skal fortsatt fungere")
		log.info("Vanlig logg skal funke")
	}
}
