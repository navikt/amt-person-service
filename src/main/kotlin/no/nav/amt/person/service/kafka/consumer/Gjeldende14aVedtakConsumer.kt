package no.nav.amt.person.service.kafka.consumer

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.amt.person.service.navbruker.HovedmalMedOkeDeltakelse
import no.nav.amt.person.service.navbruker.InnsatsgruppeV2
import no.nav.amt.person.service.navbruker.InnsatsgruppeV2.Companion.toV1
import no.nav.amt.person.service.navbruker.NavBrukerService
import no.nav.amt.person.service.person.PersonService
import no.nav.amt.person.service.utils.JsonUtils.objectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.ZonedDateTime

@Service
class Gjeldende14aVedtakConsumer(
	private val personService: PersonService,
	private val navBrukerService: NavBrukerService,
) {
	private val log = LoggerFactory.getLogger(javaClass)

	fun ingest(value: String) {
		val siste14aVedtak: Gjeldende14aVedtakKafkaPayload = objectMapper.readValue(value)

		val gjeldendeIdent = personService.hentGjeldendeIdent(siste14aVedtak.aktorId)
		val brukerId = navBrukerService.finnBrukerId(gjeldendeIdent.ident)

		if (brukerId == null) {
			log.info("Innsatsgruppe endret, men Nav-bruker finnes ikke. Hopper over Kafka-melding")
			return
		}

		navBrukerService.oppdaterInnsatsgruppe(
			brukerId,
			siste14aVedtak.innsatsgruppe.toV1(),
		)
		log.info("Oppdatert innsatsgruppe for Nav-bruker $brukerId")
	}

	data class Gjeldende14aVedtakKafkaPayload(
		val aktorId: String,
		val innsatsgruppe: InnsatsgruppeV2,
		val hovedmal: HovedmalMedOkeDeltakelse?,
		val fattetDato: ZonedDateTime,
		val vedtakId: String,
	)
}
