package no.nav.amt.person.service.person

import no.nav.amt.person.service.kafka.producer.KafkaProducerService
import no.nav.amt.person.service.person.model.Person
import no.nav.amt.person.service.person.model.Rolle
import org.springframework.stereotype.Service
import org.springframework.transaction.event.TransactionalEventListener

@Service
class ArrangorAnsattService(
	private val personService: PersonService,
	private val rolleService: RolleService,
	private val kafkaProducerService: KafkaProducerService,
) {
	fun hentEllerOpprettAnsatt(personident: String): Person {
		val person = personService.hentEllerOpprettPerson(personident)

		rolleService.opprettRolle(person.id, Rolle.ARRANGOR_ANSATT)

		return person
	}

	@TransactionalEventListener
	fun onPersonUpdate(personUpdateEvent: PersonUpdateEvent) {
		val person = personUpdateEvent.person
		if (rolleService.harRolle(person.id, Rolle.ARRANGOR_ANSATT)) {
			kafkaProducerService.publiserArrangorAnsatt(person)
		}
	}

	fun getAll(
		offset: Int,
		batchSize: Int,
	) = personService.hentAlleMedRolle(offset, batchSize, Rolle.ARRANGOR_ANSATT)
}
