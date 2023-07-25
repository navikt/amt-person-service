package no.nav.amt.person.service.kafka.producer

import no.nav.amt.person.service.kafka.config.KafkaTopicProperties
import no.nav.amt.person.service.kafka.producer.dto.NavBrukerDtoV1
import no.nav.amt.person.service.kafka.producer.dto.NavEnhetDtoV1
import no.nav.amt.person.service.nav_bruker.NavBruker
import no.nav.amt.person.service.utils.EnvUtils
import no.nav.amt.person.service.utils.JsonUtils
import no.nav.common.kafka.producer.KafkaProducerClient
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class KafkaProducerService(
	private val kafkaTopicProperties: KafkaTopicProperties,
	private val kafkaProducerClient: KafkaProducerClient<String, String>,
) {

	private val toggleProduce = !EnvUtils.isProd()

	fun publiserNavBruker(navBruker: NavBruker) {
		if (!toggleProduce) return

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
		)

		val key = navBruker.person.id.toString()
		val value =  JsonUtils.toJsonString(navBrukerDto)
		val record = ProducerRecord(kafkaTopicProperties.amtNavBrukerTopic, key, value)

		kafkaProducerClient.sendSync(record)
	}

	fun publiserSlettNavBruker(personId: UUID) {
		if (!toggleProduce) return

		val record = ProducerRecord<String, String?>(kafkaTopicProperties.amtNavBrukerTopic, personId.toString(), null)

		kafkaProducerClient.sendSync(record)
	}

}
