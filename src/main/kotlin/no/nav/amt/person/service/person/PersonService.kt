package no.nav.amt.person.service.person

import no.nav.amt.person.service.person.model.Person
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class PersonService(val repository: PersonRepository) {

	fun hentPerson(id: UUID): Person {
		return repository.get(id).toModel()
	}

}
