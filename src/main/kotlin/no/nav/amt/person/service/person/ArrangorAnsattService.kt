package no.nav.amt.person.service.person

import no.nav.amt.person.service.person.model.Person
import no.nav.amt.person.service.person.model.Rolle
import org.springframework.stereotype.Service

@Service
class ArrangorAnsattService(
	private val personService: PersonService,
	private val rolleService: RolleService,
) {
	fun hentEllerOpprettAnsatt(personident: String): Person {
		val person = personService.hentEllerOpprettPerson(personident)

		rolleService.opprettRolle(person.id, Rolle.ARRANGOR_ANSATT)

		return person
	}
}
