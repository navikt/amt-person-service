package no.nav.amt.person.service.person

import no.nav.amt.person.service.clients.pdl.PdlClient
import no.nav.amt.person.service.clients.pdl.PdlPerson
import no.nav.amt.person.service.config.SecureLog.secureLog
import no.nav.amt.person.service.person.model.IdentType
import no.nav.amt.person.service.person.model.Person
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import java.util.UUID

@Service
class PersonService(
	val pdlClient: PdlClient,
	val repository: PersonRepository,
	val applicationEventPublisher: ApplicationEventPublisher,
	val transactionTemplate: TransactionTemplate,
) {
	private val log = LoggerFactory.getLogger(javaClass)

	fun hentPerson(id: UUID): Person {
		return repository.get(id).toModel()
	}

	fun hentPerson(personIdent: String): Person? {
		return repository.get(personIdent)?.toModel()
	}

	fun hentEllerOpprettPerson(personIdent: String, nyPersonId: UUID = UUID.randomUUID()) : Person {
		return repository.get(personIdent)?.toModel() ?: opprettPerson(personIdent, nyPersonId)
	}

	fun hentEllerOpprettPerson(personIdent: String, personOpplysninger: PdlPerson): Person {
		return repository.get(personIdent)?.toModel() ?: opprettPerson(personIdent, personOpplysninger)
	}

	fun hentPersoner(personIdenter: List<String>): List<Person> {
		return repository.getPersoner(personIdenter).map { it.toModel() }
	}


	fun hentGjeldendeIdent(personIdent: String) = pdlClient.hentGjeldendePersonligIdent(personIdent)

	fun oppdaterPersonIdent(gjeldendeIdent: String, identType: IdentType, historiskeIdenter: List<String>) {
		val personer = repository.getPersoner(historiskeIdenter.plus(gjeldendeIdent))

		if(personer.size > 1) {
			secureLog.error("Vi har flere personer knyttet til ident: $gjeldendeIdent, historiske identer:$historiskeIdenter")
			log.error("Vi har flere personer knyttet til samme person")
			throw IllegalStateException("Vi har flere personer knyttet til samme person")
		}

		personer.firstOrNull()?.let { person ->
			upsert(person.copy(
				personIdent = gjeldendeIdent,
				personIdentType = identType,
				historiskeIdenter = historiskeIdenter
			).toModel())
		}

	}

	fun upsert(person: Person) {
		transactionTemplate.executeWithoutResult {
			repository.upsert(person)
			applicationEventPublisher.publishEvent(PersonUpdateEvent(person))

			log.info("Upsertet person med id: ${person.id}")
		}
	}

	private fun opprettPerson(personIdent: String, nyPersonId: UUID): Person {
		val pdlPerson =	pdlClient.hentPerson(personIdent)

		return opprettPerson(personIdent, pdlPerson, nyPersonId)
	}

	private fun opprettPerson(personIdent: String, pdlPerson: PdlPerson, nyPersonId: UUID = UUID.randomUUID()): Person {
		val personIdentType = pdlPerson.identer.first { it.ident == personIdent }.gruppe

		val person = Person(
			id = nyPersonId,
			personIdent = personIdent,
			personIdentType = IdentType.valueOf(personIdentType),
			historiskeIdenter = pdlPerson.identer.filter { it.ident != personIdent }.map { it.ident },
			fornavn = pdlPerson.fornavn,
			mellomnavn = pdlPerson.mellomnavn,
			etternavn = pdlPerson.etternavn,
		)

		upsert(person)

		log.info("Opprettet ny person med id ${person.id}")

		return person
	}

	fun slettPerson(person: Person) {
		repository.delete(person.id)

		secureLog.info("Slettet person med ident: ${person.personIdent}")
		log.info("Slettet person med id: ${person.id}")
	}

}
