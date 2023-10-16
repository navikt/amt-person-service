package no.nav.amt.person.service.kafka.producer

import no.nav.amt.person.service.kafka.config.KafkaTopicProperties
import no.nav.amt.person.service.kafka.producer.dto.ArrangorAnsattDtoV1
import no.nav.amt.person.service.kafka.producer.dto.NavAnsattDtoV1
import no.nav.amt.person.service.kafka.producer.dto.NavBrukerDtoV1
import no.nav.amt.person.service.kafka.producer.dto.NavEnhetDtoV1
import no.nav.amt.person.service.nav_ansatt.NavAnsatt
import no.nav.amt.person.service.nav_bruker.NavBruker
import no.nav.amt.person.service.person.model.Person
import no.nav.amt.person.service.utils.JsonUtils.toJsonString
import no.nav.common.kafka.producer.KafkaProducerClient
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class KafkaProducerService(
	private val kafkaTopicProperties: KafkaTopicProperties,
	private val kafkaProducerClient: KafkaProducerClient<String, String>,
) {

	fun publiserNavBruker(navBruker: NavBruker) {

		val navBrukerDto = NavBrukerDtoV1(
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
			adressebeskyttelse = navBruker.adressebeskyttelse
		)

		val key = navBruker.person.id.toString()
		val value =  toJsonString(navBrukerDto)
		val record = ProducerRecord(kafkaTopicProperties.amtNavBrukerTopic, key, value)

		kafkaProducerClient.sendSync(record)
	}

	fun publiserSlettNavBruker(personId: UUID) {

		val record = ProducerRecord<String, String?>(kafkaTopicProperties.amtNavBrukerTopic, personId.toString(), null)

		kafkaProducerClient.sendSync(record)
	}

	fun publiserArrangorAnsatt(ansatt: Person) {
		val key = ansatt.id.toString()
		val value = toJsonString(
			ArrangorAnsattDtoV1(
				id = ansatt.id,
				personident = ansatt.personident,
				fornavn = ansatt.fornavn,
				mellomnavn = ansatt.mellomnavn,
				etternavn = ansatt.etternavn,
			)
		)

		kafkaProducerClient.sendSync(ProducerRecord(kafkaTopicProperties.amtArrangorAnsattPersonaliaTopic, key, value))
	}

	fun publiserNavAnsatt(ansatt: NavAnsatt) {
		val key = ansatt.id.toString()
		val value = toJsonString(
			NavAnsattDtoV1(
				id = ansatt.id,
				navident = ansatt.navIdent,
				navn = ansatt.navn,
				telefon = ansatt.telefon,
				epost = ansatt.epost,
			)
		)

		kafkaProducerClient.sendSync(ProducerRecord(kafkaTopicProperties.amtNavAnsattPersonaliaTopic, key, value))
	}
}
