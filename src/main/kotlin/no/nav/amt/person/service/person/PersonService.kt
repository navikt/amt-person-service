package no.nav.amt.person.service.person

import no.nav.amt.person.service.clients.pdl.PdlClient
import no.nav.amt.person.service.clients.pdl.PdlPerson
import no.nav.amt.person.service.person.model.AdressebeskyttelseGradering
import no.nav.amt.person.service.person.model.Person
import no.nav.amt.person.service.person.model.Personident
import no.nav.amt.person.service.person.model.Rolle
import no.nav.amt.person.service.person.model.finnGjeldendeIdent
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import java.util.UUID

@Service
class PersonService(
	val pdlClient: PdlClient,
	val repository: PersonRepository,
	val personidentRepository: PersonidentRepository,
	val applicationEventPublisher: ApplicationEventPublisher,
	val transactionTemplate: TransactionTemplate,
) {
	private val log = LoggerFactory.getLogger(javaClass)

	fun hentPerson(id: UUID): Person {
		return repository.get(id).toModel()
	}

	fun hentPerson(personident: String): Person? {
		return repository.get(personident)?.toModel()
	}

	@Retryable(maxAttempts = 2)
	fun hentEllerOpprettPerson(personident: String): Person {
		return repository.get(personident)?.toModel() ?: opprettPerson(personident)
	}

	fun hentEllerOpprettPerson(personident: String, personOpplysninger: PdlPerson): Person {
		return repository.get(personident)?.toModel() ?: opprettPerson(personOpplysninger)
	}

	fun hentPersoner(personidenter: List<String>): List<Person> {
		return repository.getPersoner(personidenter).map { it.toModel() }
	}

	fun hentIdenter(personId: UUID) = personidentRepository.getAllForPerson(personId).map { it.toModel() }

	fun hentIdenter(personident: String) = pdlClient.hentIdenter(personident)

	fun hentGjeldendeIdent(personident: String) = finnGjeldendeIdent(pdlClient.hentIdenter(personident)).getOrThrow()

	fun oppdaterPersonIdent(identer: List<Personident>) {
		val personer = repository.getPersoner(identer.map { it.ident })

		if (personer.size > 1) {
			log.error("Vi har flere personer knyttet til samme identer: ${personer.joinToString { it.id.toString() }}")
			throw IllegalStateException("Vi har flere personer knyttet til samme identer")
		}

		val gjeldendeIdent = finnGjeldendeIdent(identer).getOrThrow()

		personer.firstOrNull()?.let { person ->
			upsert(person.copy(personident = gjeldendeIdent.ident).toModel())
			personidentRepository.upsert(person.id, identer)
		}

	}

	fun upsert(person: Person) {
		transactionTemplate.executeWithoutResult {
			repository.upsert(person)
			applicationEventPublisher.publishEvent(PersonUpdateEvent(person))

			log.info("Upsertet person med id: ${person.id}")
		}
	}

	private fun opprettPerson(personident: String): Person {
		val pdlPerson = pdlClient.hentPerson(personident)

		return opprettPerson(pdlPerson)
	}

	private fun opprettPerson(pdlPerson: PdlPerson): Person {
		val gjeldendeIdent = finnGjeldendeIdent(pdlPerson.identer).getOrThrow()

		val person = Person(
			id = UUID.randomUUID(),
			personident = gjeldendeIdent.ident,
			fornavn = pdlPerson.fornavn,
			mellomnavn = pdlPerson.mellomnavn,
			etternavn = pdlPerson.etternavn,
		)

		upsert(person)
		personidentRepository.upsert(person.id, pdlPerson.identer)

		log.info("Opprettet ny person med id ${person.id}")

		return person
	}

	fun slettPerson(person: Person) {
		repository.delete(person.id)

		log.info("Slettet person med id: ${person.id}")
	}

	fun hentAdressebeskyttelse(personident: String): AdressebeskyttelseGradering? {
		return pdlClient.hentAdressebeskyttelse(personident)
	}

	fun hentAlleMedRolle(offset: Int, limit: Int = 500, rolle: Rolle) =
		repository.getAllWithRolle(offset, limit, rolle)
			.map { it.toModel() }


}
