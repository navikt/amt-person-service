package no.nav.amt.person.service.data.kafka

import no.nav.amt.person.service.data.TestData
import no.nav.amt.person.service.data.kafka.message.EndringPaaBrukerMsg

object KafkaMessageCreator {
	fun lagEndringPaaBrukerMsg(
		fodselsnummer: String = TestData.randomIdent(),
		oppfolgingsenhet: String? = TestData.randomEnhetId(),
	) = EndringPaaBrukerMsg(
		fodlsesnummer = fodselsnummer,
		oppfolgingsenhet = oppfolgingsenhet,
	)
}
