package no.nav.amt.person.service.kafka.consumer

import no.nav.amt.person.service.kafka.consumer.dto.SisteOppfolgingsperiodeKafkaPayload
import no.nav.amt.person.service.navbruker.NavBrukerService
import no.nav.amt.person.service.person.PersonService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue

@Component
class OppfolgingsperiodeConsumer(
	private val personService: PersonService,
	private val navBrukerService: NavBrukerService,
	private val objectMapper: ObjectMapper,
) {
	private val log = LoggerFactory.getLogger(javaClass)

	fun ingest(value: String) {
		val sisteOppfolgingsperiode = objectMapper.readValue<SisteOppfolgingsperiodeKafkaPayload>(value)

		val gjeldendeIdent =
			runCatching {
				personService.hentGjeldendeIdent(sisteOppfolgingsperiode.aktorId)
			}.getOrElse { throwable ->
				// midlertidig fiks i og med at GraphQL ikke returnerer 404
				if (throwable.message?.contains("Fant ikke person") == true) {
					log.warn(throwable.message, throwable)
					return
				} else {
					throw throwable
				}
			}

		val brukerId = navBrukerService.finnBrukerId(gjeldendeIdent.ident)

		if (brukerId == null) {
			log.info("Nav-bruker finnes ikke i tabellen nav_bruker, dropper videre prosessering")
			return
		}

		navBrukerService.oppdaterOppfolgingsperiodeOgInnsatsgruppe(
			brukerId,
			sisteOppfolgingsperiode.toOppfolgingsperiode(),
		)

		log.info("Oppdatert oppfølgingsperiode med id ${sisteOppfolgingsperiode.uuid} for bruker $brukerId")
	}
}
