package no.nav.amt.person.service.person

import no.nav.amt.person.service.clients.pdl.PdlClient
import no.nav.amt.person.service.config.SecureLog.secureLog
import no.nav.amt.person.service.person.model.AdressebeskyttelseGradering
import no.nav.amt.person.service.person.model.IdentType
import no.nav.amt.person.service.person.model.Person
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*

@Service
class PersonService(
	val pdlClient: PdlClient,
	val repository: PersonRepository,
) {
	private val log = LoggerFactory.getLogger(javaClass)

	fun hentPerson(id: UUID): Person {
		return repository.get(id).toModel()
	}

	fun hentPerson(personIdent: String): Person? {
		return repository.get(personIdent)?.toModel()
	}

	fun hentEllerOpprettPerson(personIdent: String) : Person {
		return repository.get(personIdent)?.toModel() ?: opprettPerson(personIdent)
	}

	fun hentPersoner(personIdenter: List<String>): List<Person> {
		return repository.getPersoner(personIdenter).map { it.toModel() }
	}

	fun hentGjeldendeIdent(personIdent: String) = pdlClient.hentGjeldendePersonligIdent(personIdent)

	fun erAdressebeskyttet(personIdent: String): Boolean {
		val gradering = pdlClient.hentAdressebeskyttelse(personIdent)

		return gradering != null && gradering != AdressebeskyttelseGradering.UGRADERT
	}

	fun oppdaterPersonIdent(gjeldendeIdent: String, identType: IdentType, historiskeIdenter: List<String>) {
		val personer = repository.getPersoner(historiskeIdenter.plus(gjeldendeIdent))

		if(personer.size > 1) {
			secureLog.error("Vi har flere personer knyttet til ident: $gjeldendeIdent, historiske identer:$historiskeIdenter")
			log.error("Vi har flere personer knyttet til samme person")
			throw IllegalStateException("Vi har flere personer knyttet til samme person")
		}

		personer.firstOrNull()?.let { person ->
			repository.oppdaterIdenter(person.id, gjeldendeIdent, identType, historiskeIdenter)
		}

	}

	fun oppdaterPerson(person: Person) {
		repository.upsert(person)
		log.info("Oppdaterte person med id: ${person.id}")
	}

	private fun opprettPerson(personIdent: String): Person {
		val pdlPerson =	pdlClient.hentPerson(personIdent)
		val personIdentType = pdlPerson.identer.first { it.ident == personIdent }.gruppe

		val person = Person(
			id = UUID.randomUUID(),
			personIdent = personIdent,
			personIdentType = IdentType.valueOf(personIdentType),
			historiskeIdenter = pdlPerson.identer.filter { it.ident != personIdent }.map { it.ident },
			fornavn = pdlPerson.fornavn,
			mellomnavn = pdlPerson.mellomnavn,
			etternavn = pdlPerson.etternavn,
		)

		repository.upsert(person)

		log.info("Opprettet ny person med id ${person.id}")

		return person
	}

	fun slettPerson(person: Person) {
		repository.delete(person.id)

		secureLog.info("Slettet person med ident: ${person.personIdent}")
		log.info("Slettet person med id: ${person.id}")
	}

}
