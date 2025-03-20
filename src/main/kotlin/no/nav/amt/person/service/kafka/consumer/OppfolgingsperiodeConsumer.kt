package no.nav.amt.person.service.kafka.consumer

import no.nav.amt.person.service.nav_bruker.NavBrukerService
import no.nav.amt.person.service.nav_bruker.Oppfolgingsperiode
import no.nav.amt.person.service.person.PersonService
import no.nav.amt.person.service.utils.JsonUtils.fromJsonString
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.ZonedDateTime
import java.util.UUID

@Service
class OppfolgingsperiodeConsumer(
	private val personService: PersonService,
	private val navBrukerService: NavBrukerService,
) {

	private val log = LoggerFactory.getLogger(javaClass)

	fun ingest(value: String) {
		val sisteOppfolgingsperiode = fromJsonString<SisteOppfolgingsperiodeV1>(value)

		val gjeldendeIdent = personService.hentGjeldendeIdent(sisteOppfolgingsperiode.aktorId)
		val brukerId = navBrukerService.finnBrukerId(gjeldendeIdent.ident)

		if (brukerId == null) {
			log.info("Oppfølgingsperiode endret. NavBruker finnes ikke, hopper over kafka melding")
			return
		}

		navBrukerService.oppdaterOppfolgingsperiode(
			brukerId,
			Oppfolgingsperiode(
				id = sisteOppfolgingsperiode.uuid,
				startdato = sisteOppfolgingsperiode.startDato.toLocalDateTime(),
				sluttdato = sisteOppfolgingsperiode.sluttDato?.toLocalDateTime()
			)
		)
		log.info("Oppdatert oppfølgingsperiode med id ${sisteOppfolgingsperiode.uuid} for bruker $brukerId")
	}

	data class SisteOppfolgingsperiodeV1(
		val uuid: UUID,
		val aktorId: String,
		val startDato: ZonedDateTime,
		val sluttDato: ZonedDateTime?,
	)
}
