package no.nav.amt.person.service.person

import no.nav.amt.person.service.clients.amt_tiltak.AmtTiltakClient
import no.nav.amt.person.service.person.model.Person
import no.nav.amt.person.service.person.model.Rolle
import org.springframework.stereotype.Service

@Service
class ArrangorAnsattService(
	private val personService: PersonService,
	private val rolleService: RolleService,
	private val amtTiltakClient: AmtTiltakClient,
) {
	fun hentEllerOpprettAnsatt(personident: String): Person {
		var person = personService.hentPerson(personident)

		if (person == null) {
			val brukerId = amtTiltakClient.hentBrukerId(personident)
			person = if (brukerId != null) {
				personService.opprettPersonMedId(personident, brukerId)
			} else {
				personService.hentEllerOpprettPerson(personident)
			}
		}

		rolleService.opprettRolle(person.id, Rolle.ARRANGOR_ANSATT)

		return person
	}
}
