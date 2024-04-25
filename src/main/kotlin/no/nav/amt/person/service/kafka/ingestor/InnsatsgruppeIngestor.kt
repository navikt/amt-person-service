package no.nav.amt.person.service.kafka.ingestor

import no.nav.amt.person.service.clients.veilarbvedtaksstotte.VeilarbvedtaksstotteClient
import no.nav.amt.person.service.clients.veilarbvedtaksstotte.toInnsatsgruppe
import no.nav.amt.person.service.nav_bruker.NavBrukerService
import no.nav.amt.person.service.person.PersonService
import no.nav.amt.person.service.utils.JsonUtils.fromJsonString
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class InnsatsgruppeIngestor(
	private val personService: PersonService,
	private val navBrukerService: NavBrukerService,
) {

	private val log = LoggerFactory.getLogger(javaClass)

	fun ingest(value: String) {
		val siste14aVedtak = fromJsonString<Siste14aVedtak>(value)

		val gjeldendeIdent = personService.hentGjeldendeIdent(siste14aVedtak.aktorId)
		val brukerId = navBrukerService.finnBrukerId(gjeldendeIdent.ident)

		if (brukerId == null) {
			log.info("Innsatsgruppe endret. NavBruker finnes ikke, hopper over kafkamelding")
			return
		}

		navBrukerService.oppdaterInnsatsgruppe(
			brukerId,
			siste14aVedtak.innsatsgruppe.toInnsatsgruppe()
		)
		log.info("Oppdatert innsatsgruppe for bruker $brukerId")
	}

	data class Siste14aVedtak(
		val aktorId: String,
		val innsatsgruppe: VeilarbvedtaksstotteClient.InnsatsgruppeDto
	)
}
