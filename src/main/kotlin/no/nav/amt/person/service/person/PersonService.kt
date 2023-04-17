package no.nav.amt.person.service.person

import no.nav.amt.person.service.clients.pdl.PdlClient
import no.nav.amt.person.service.person.dbo.PersonUpsert
import no.nav.amt.person.service.person.model.IdentType
import no.nav.amt.person.service.person.model.Person
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class PersonService(
	val pdlClient: PdlClient,
	val repository: PersonRepository,
) {

	fun hentPerson(id: UUID): Person {
		return repository.get(id).toModel()
	}

	fun hentPerson(personIdent: String) : Person {
		return repository.get(personIdent)?.toModel() ?: opprettPerson(personIdent)
	}

	private fun opprettPerson(personIdent: String): Person {
		val pdlPerson =	pdlClient.hentPerson(personIdent)
		val personIdentType = pdlPerson.identer.first { it.ident == personIdent }.gruppe

		val personUpsert = PersonUpsert(
			id = UUID.randomUUID(),
			personIdent = personIdent,
			personIdentType = IdentType.valueOf(personIdentType),
			historiskeIdenter = pdlPerson.identer.filter { it.ident != personIdent }.map { it.ident },
			fornavn = pdlPerson.fornavn,
			mellomnavn = pdlPerson.mellomnavn,
			etternavn = pdlPerson.etternavn,
		)

		repository.upsert(personUpsert)

		return Person(
			personUpsert.id,
			personUpsert.personIdent,
			personUpsert.personIdentType,
			personUpsert.fornavn,
			personUpsert.mellomnavn,
			personUpsert.etternavn,
			personUpsert.historiskeIdenter
		)
	}

}
