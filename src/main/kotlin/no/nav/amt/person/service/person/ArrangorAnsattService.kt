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
	fun hentEllerOpprettAnsatt(personIdent: String): Person {
		var person = personService.hentPerson(personIdent)

		if (person == null) {
			val brukerId = amtTiltakClient.hentBrukerId(personIdent)
			person = if (brukerId != null) {
				personService.opprettPersonMedId(personIdent, brukerId)
			} else {
				personService.hentEllerOpprettPerson(personIdent)
			}
		}

		rolleService.opprettRolle(person.id, Rolle.ARRANGOR_ANSATT)

		return person
	}
}
