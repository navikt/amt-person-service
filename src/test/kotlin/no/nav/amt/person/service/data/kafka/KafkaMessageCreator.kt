package no.nav.amt.person.service.data.kafka

import no.nav.amt.person.service.data.TestData
import no.nav.amt.person.service.data.kafka.message.EndringPaaBrukerMsg
import no.nav.amt.person.service.data.kafka.message.TildeltVeilederMsg
import java.time.ZonedDateTime

object KafkaMessageCreator {
	fun lagEndringPaaBrukerMsg(
		fodselsnummer: String = TestData.randomIdent(),
		oppfolgingsenhet: String? = TestData.randomEnhetId(),
	) = EndringPaaBrukerMsg(
		fodlsesnummer = fodselsnummer,
		oppfolgingsenhet = oppfolgingsenhet,
	)

	fun lagTildeltVeilederMsg(
		aktorId: String = TestData.randomIdent(),
		veilederId: String = TestData.randomNavIdent(),
		tilordnet: ZonedDateTime = ZonedDateTime.now(),
	) = TildeltVeilederMsg(
		aktorId = aktorId,
		veilederId = veilederId,
		tilordnet = tilordnet
	)

}
