package no.nav.amt.person.service.kafka.producer

import no.nav.amt.person.service.api.dto.toDto
import no.nav.amt.person.service.kafka.config.KafkaTopicProperties
import no.nav.amt.person.service.kafka.producer.dto.ArrangorAnsattDtoV1
import no.nav.amt.person.service.kafka.producer.dto.NavAnsattDtoV1
import no.nav.amt.person.service.kafka.producer.dto.NavBrukerDtoV1
import no.nav.amt.person.service.kafka.producer.dto.NavEnhetDtoV1
import no.nav.amt.person.service.navansatt.NavAnsatt
import no.nav.amt.person.service.navbruker.NavBruker
import no.nav.amt.person.service.navenhet.NavEnhet
import no.nav.amt.person.service.person.model.Person
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

	fun publiserNavBruker(navBruker: NavBruker) {
		val navBrukerDto =
			NavBrukerDtoV1(
				personId = navBruker.person.id,
				personident = navBruker.person.personident,
				fornavn = navBruker.person.fornavn,
				mellomnavn = navBruker.person.mellomnavn,
				etternavn = navBruker.person.etternavn,
				navVeilederId = navBruker.navVeileder?.id,
				navEnhet = navBruker.navEnhet?.let { NavEnhetDtoV1(it.id, it.enhetId, it.navn) },
				telefon = navBruker.telefon,
				epost = navBruker.epost,
				erSkjermet = navBruker.erSkjermet,
				adresse = navBruker.adresse,
				adressebeskyttelse = navBruker.adressebeskyttelse,
				oppfolgingsperioder = navBruker.oppfolgingsperioder,
				innsatsgruppe = navBruker.innsatsgruppe,
			)

		val key = navBruker.person.id.toString()
		val value = objectMapper.writeValueAsString(navBrukerDto)
		val record = ProducerRecord(kafkaTopicProperties.amtNavBrukerTopic, key, value)

		kafkaProducerClient.sendSync(record)
		log.info("Publiserte navbruker med personId ${navBrukerDto.personId} til topic")
	}

	// brukes kun av tester
	fun publiserSlettNavBruker(personId: UUID) {
		val record = ProducerRecord<String, String?>(kafkaTopicProperties.amtNavBrukerTopic, personId.toString(), null)

		kafkaProducerClient.sendSync(record)
	}

	fun publiserArrangorAnsatt(ansatt: Person) {
		val key = ansatt.id.toString()
		val value =
			objectMapper.writeValueAsString(
				ArrangorAnsattDtoV1(
					id = ansatt.id,
					personident = ansatt.personident,
					fornavn = ansatt.fornavn,
					mellomnavn = ansatt.mellomnavn,
					etternavn = ansatt.etternavn,
				),
			)

		kafkaProducerClient.sendSync(ProducerRecord(kafkaTopicProperties.amtArrangorAnsattPersonaliaTopic, key, value))
	}

	fun publiserNavAnsatt(ansatt: NavAnsatt) {
		val key = ansatt.id.toString()
		val value =
			objectMapper.writeValueAsString(
				NavAnsattDtoV1(
					id = ansatt.id,
					navident = ansatt.navIdent,
					navn = ansatt.navn,
					telefon = ansatt.telefon,
					epost = ansatt.epost,
					navEnhetId = ansatt.navEnhetId,
				),
			)

		kafkaProducerClient.sendSync(ProducerRecord(kafkaTopicProperties.amtNavAnsattPersonaliaTopic, key, value))
	}

	fun publiserNavEnhet(navEnhet: NavEnhet) {
		val key = navEnhet.id.toString()
		val value = objectMapper.writeValueAsString(navEnhet.toDto())

		kafkaProducerClient.sendSync(ProducerRecord(kafkaTopicProperties.amtNavEnhetTopic, key, value))
		log.info("Publiserte nav enhet med id ${navEnhet.id} til topic")
	}
}
