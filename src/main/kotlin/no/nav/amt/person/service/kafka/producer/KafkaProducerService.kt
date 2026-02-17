package no.nav.amt.person.service.kafka.producer

import no.nav.amt.person.service.kafka.config.KafkaTopicProperties
import no.nav.amt.person.service.kafka.producer.dto.ArrangorAnsattDtoV1
import no.nav.amt.person.service.kafka.producer.dto.NavAnsattDtoV1
import no.nav.amt.person.service.kafka.producer.dto.NavBrukerDtoV1
import no.nav.amt.person.service.kafka.producer.dto.NavEnhetDtoV1
import no.nav.amt.person.service.navansatt.NavAnsattDbo
import no.nav.amt.person.service.navbruker.dbo.NavBrukerDbo
import no.nav.amt.person.service.navenhet.NavEnhetDbo
import no.nav.amt.person.service.person.dbo.PersonDbo
import no.nav.common.kafka.producer.KafkaProducerClient
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper
import java.util.UUID

@Service
class KafkaProducerService(
	private val kafkaTopicProperties: KafkaTopicProperties,
	private val kafkaProducerClient: KafkaProducerClient<String, String>,
	private val objectMapper: ObjectMapper,
) {
	private val log = LoggerFactory.getLogger(javaClass)

	fun publiserNavBruker(navBruker: NavBrukerDbo) {
		kafkaProducerClient.sendSync(
			ProducerRecord(
				kafkaTopicProperties.amtNavBrukerTopic,
				navBruker.person.id.toString(),
				objectMapper.writeValueAsString(NavBrukerDtoV1.fromDbo(navBruker)),
			),
		)
		log.info("Publiserte navbruker med personId ${navBruker.person.id} til topic")
	}

	fun publiserArrangorAnsatt(ansatt: PersonDbo) {
		kafkaProducerClient.sendSync(
			ProducerRecord(
				kafkaTopicProperties.amtArrangorAnsattPersonaliaTopic,
				ansatt.id.toString(),
				objectMapper.writeValueAsString(ArrangorAnsattDtoV1.fromDbo(ansatt)),
			),
		)
	}

	fun publiserNavAnsatt(ansatt: NavAnsattDbo) {
		kafkaProducerClient.sendSync(
			ProducerRecord(
				kafkaTopicProperties.amtNavAnsattPersonaliaTopic,
				ansatt.id.toString(),
				objectMapper.writeValueAsString(NavAnsattDtoV1.fromDbo(ansatt)),
			),
		)
	}

	fun publiserNavEnhet(navEnhet: NavEnhetDbo) {
		kafkaProducerClient.sendSync(
			ProducerRecord(
				kafkaTopicProperties.amtNavEnhetTopic,
				navEnhet.id.toString(),
				objectMapper.writeValueAsString(NavEnhetDtoV1.fromDbo(navEnhet)),
			),
		)
		log.info("Publiserte nav enhet med id ${navEnhet.id} til topic")
	}

	// brukes kun av tester
	internal fun publiserSlettNavBruker(personId: UUID) {
		kafkaProducerClient.sendSync(
			ProducerRecord(
				kafkaTopicProperties.amtNavBrukerTopic,
				personId.toString(),
				null,
			),
		)
	}
}
