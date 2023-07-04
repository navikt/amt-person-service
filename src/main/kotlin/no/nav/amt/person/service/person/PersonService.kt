package no.nav.amt.person.service.person

import no.nav.amt.person.service.clients.pdl.PdlClient
import no.nav.amt.person.service.clients.pdl.PdlPerson
import no.nav.amt.person.service.config.SecureLog.secureLog
import no.nav.amt.person.service.person.model.Person
import no.nav.amt.person.service.person.model.Personident
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

	fun opprettPersonMedId(personIdent: String, nyPersonId: UUID) : Person {
		return opprettPerson(personIdent, nyPersonId)
	}

	@Retryable(maxAttempts = 2)
	fun hentEllerOpprettPerson(personIdent: String) : Person {
		return repository.get(personIdent)?.toModel() ?: opprettPerson(personIdent)
	}

	fun hentEllerOpprettPerson(personIdent: String, personOpplysninger: PdlPerson): Person {
		return repository.get(personIdent)?.toModel() ?: opprettPerson(personOpplysninger)
	}

	fun hentPersoner(personIdenter: List<String>): List<Person> {
		return repository.getPersoner(personIdenter).map { it.toModel() }
	}

	fun hentIdenter(personIdent: String) = pdlClient.hentIdenter(personIdent)

	fun hentGjeldendeIdent(personIdent: String) = finnGjeldendeIdent(pdlClient.hentIdenter(personIdent)).getOrThrow()

	fun oppdaterPersonIdent(identer: List<Personident>) {
		val personer = repository.getPersoner(identer.map { it.ident })

		if(personer.size > 1) {
			secureLog.error("Vi har flere personer knyttet til identer: $identer")
			log.error("Vi har flere personer knyttet til samme identer: ${personer.joinToString { it.id.toString() }}")
			throw IllegalStateException("Vi har flere personer knyttet til samme identer")
		}

		val gjeldendeIdent = finnGjeldendeIdent(identer).getOrThrow()

		personer.firstOrNull()?.let { person ->
			upsert(person.copy(
				personIdent = gjeldendeIdent.ident,
				personIdentType = gjeldendeIdent.type,
				historiskeIdenter = identer.filter { it != gjeldendeIdent }.map { it.ident }
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

	private fun opprettPerson(personIdent: String, nyPersonId: UUID = UUID.randomUUID()): Person {
		val pdlPerson =	pdlClient.hentPerson(personIdent)

		return opprettPerson(pdlPerson, nyPersonId)
	}

	private fun opprettPerson(pdlPerson: PdlPerson, nyPersonId: UUID = UUID.randomUUID()): Person {
		val gjeldendeIdent = finnGjeldendeIdent(pdlPerson.identer).getOrThrow()

		val person = Person(
			id = nyPersonId,
			personIdent = gjeldendeIdent.ident,
			personIdentType = gjeldendeIdent.type,
			historiskeIdenter = pdlPerson.identer.filter { it.ident != gjeldendeIdent.ident }.map { it.ident },
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
