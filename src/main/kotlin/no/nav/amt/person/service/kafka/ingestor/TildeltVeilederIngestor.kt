package no.nav.amt.person.service.kafka.ingestor

import no.nav.amt.person.service.kafka.ingestor.dto.SisteTildeltVeilederDto
import no.nav.amt.person.service.nav_ansatt.NavAnsattService
import no.nav.amt.person.service.nav_bruker.NavBrukerService
import no.nav.amt.person.service.person.PersonService
import no.nav.amt.person.service.utils.JsonUtils.fromJsonString
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class TildeltVeilederIngestor(
	private val personService: PersonService,
	private val navBrukerService: NavBrukerService,
	private val navAnsattService: NavAnsattService,
) {

	private val log = LoggerFactory.getLogger(javaClass)

	fun ingest(value: String) {
		val sisteTildeltVeileder = fromJsonString<SisteTildeltVeilederDto>(value)

		val gjeldendeIdent = personService.hentGjeldendeIdent(sisteTildeltVeileder.aktorId)
		val brukerId = navBrukerService.finnBrukerId(gjeldendeIdent.ident)

		if (brukerId == null) {
			log.info("Tildelt veileder endret. NavBruker finnes ikke, hopper over kafka melding")
			return
		}

		val veileder = navAnsattService.hentEllerOpprettAnsatt(sisteTildeltVeileder.veilederId)

		navBrukerService.oppdaterNavVeileder(brukerId, veileder)
		log.info("Tildelt veileder endret. Veileder ${veileder.id} tildelt til bruker $brukerId")
	}
}
