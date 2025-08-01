package no.nav.amt.person.service.kafka.consumer

import no.nav.amt.person.service.navbruker.NavBrukerService
import no.nav.amt.person.service.utils.JsonUtils.fromJsonString
import org.springframework.stereotype.Service

@Service
class SkjermetPersonConsumer(
	private val navBrukerService: NavBrukerService,
) {
	fun ingest(
		key: String,
		value: String,
	) {
		val erSkjermet = fromJsonString<Boolean>(value)

		val brukerId = navBrukerService.finnBrukerId(key) ?: return

		navBrukerService.settSkjermet(brukerId, erSkjermet)
	}
}
