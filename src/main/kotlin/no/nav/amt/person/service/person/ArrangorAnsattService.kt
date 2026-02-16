package no.nav.amt.person.service.person

import no.nav.amt.person.service.kafka.producer.KafkaProducerService
import no.nav.amt.person.service.person.model.Person
import no.nav.amt.person.service.person.model.Rolle
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ArrangorAnsattService(
	private val personRepository: PersonRepository,
	private val personService: PersonService,
	private val rolleRepository: RolleRepository,
	private val kafkaProducerService: KafkaProducerService,
) {
	@Transactional
	fun hentEllerOpprettAnsatt(personident: String): Person {
		val person = personService.hentEllerOpprettPerson(personident)
		rolleRepository.insert(person.id, Rolle.ARRANGOR_ANSATT)
		return person
	}

	@EventListener
	fun onPersonUpdate(personUpdateEvent: PersonUpdateEvent) {
		val person = personUpdateEvent.person
		if (rolleRepository.harRolle(person.id, Rolle.ARRANGOR_ANSATT)) {
			kafkaProducerService.publiserArrangorAnsatt(person)
		}
	}

	fun getAll(
		offset: Int,
		batchSize: Int,
	): List<Person> =
		personRepository
			.getAllWithRolle(offset, batchSize, Rolle.ARRANGOR_ANSATT)
			.map { it.toModel() }
}
