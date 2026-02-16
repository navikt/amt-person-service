package no.nav.amt.person.service.person

import no.nav.amt.person.service.clients.pdl.PdlClient
import no.nav.amt.person.service.clients.pdl.PdlPerson
import no.nav.amt.person.service.person.model.AdressebeskyttelseGradering
import no.nav.amt.person.service.person.model.Person
import no.nav.amt.person.service.person.model.Personident
import no.nav.amt.person.service.person.model.Rolle
import no.nav.amt.person.service.person.model.finnGjeldendeIdent
import no.nav.amt.person.service.utils.EnvUtils
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.resilience.annotation.Retryable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class PersonService(
	val pdlClient: PdlClient,
	val repository: PersonRepository,
	val personidentRepository: PersonidentRepository,
	val applicationEventPublisher: ApplicationEventPublisher,
) {
	private val log = LoggerFactory.getLogger(javaClass)

	fun hentPerson(id: UUID): Person = repository.get(id).toModel()

	fun hentPerson(personident: String): Person? = repository.get(personident)?.toModel()

	@Retryable(maxRetries = 2)
	@Transactional
	fun hentEllerOpprettPerson(personident: String): Person = repository.get(personident)?.toModel() ?: opprettPerson(personident)

	@Transactional
	fun hentEllerOpprettPerson(
		personident: String,
		personOpplysninger: PdlPerson,
	): Person = repository.get(personident)?.toModel() ?: opprettPerson(personOpplysninger)

	fun hentPersoner(personidenter: Set<String>): List<Person> = repository.getPersoner(personidenter).map { it.toModel() }

	fun hentIdenter(personId: UUID) = personidentRepository.getAllForPerson(personId).map { it.toModel() }

	fun hentIdenter(personident: String) = pdlClient.hentIdenter(personident)

	fun hentGjeldendeIdent(personident: String) = finnGjeldendeIdent(pdlClient.hentIdenter(personident)).getOrThrow()

	@Transactional
	fun oppdaterPersonIdent(identer: List<Personident>) {
		val personer = repository.getPersoner(identer.map { it.ident }.toSet())

		if (personer.size > 1) {
			log.error("Vi har flere personer knyttet til samme identer: ${personer.joinToString { it.id.toString() }}")
			throw IllegalStateException("Vi har flere personer knyttet til samme identer")
		}

		val gjeldendeIdent = finnGjeldendeIdent(identer).getOrThrow()

		personer.firstOrNull()?.let { person ->
			log.info("Oppdaterer personident for person ${person.id}")
			personidentRepository.upsert(person.id, identer)
			upsert(person.copy(personident = gjeldendeIdent.ident).toModel())
		}
	}

	@Transactional
	fun oppdaterNavn(person: Person) {
		val personOpplysninger =
			try {
				pdlClient.hentPerson(person.personident)
			} catch (e: Exception) {
				val feilmelding = "Klarte ikke hente person ${person.id} fra PDL ved oppdatert navn: ${e.message}"

				if (EnvUtils.isDev()) {
					log.info(feilmelding)
					return
				} else {
					log.error(feilmelding, e)
					throw RuntimeException(feilmelding, e)
				}
			}

		if (
			person.fornavn == personOpplysninger.fornavn &&
			person.mellomnavn == personOpplysninger.mellomnavn &&
			person.etternavn == personOpplysninger.etternavn
		) {
			log.info("Navn på person ${person.id} er allerede oppdatert, ingen endringer gjort.")
			return
		}

		upsert(
			person.copy(
				fornavn = personOpplysninger.fornavn,
				mellomnavn = personOpplysninger.mellomnavn,
				etternavn = personOpplysninger.etternavn,
			),
		)

		log.info("Oppdaterte navn på person ${person.id}")
	}

	fun upsert(person: Person) {
		repository.upsert(person)
		applicationEventPublisher.publishEvent(PersonUpdateEvent(person))

		log.info("Upsertet person med id: ${person.id}")
	}

	private fun opprettPerson(personident: String): Person {
		val pdlPerson = pdlClient.hentPerson(personident)

		return opprettPerson(pdlPerson)
	}

	private fun opprettPerson(pdlPerson: PdlPerson): Person {
		val gjeldendeIdent = finnGjeldendeIdent(pdlPerson.identer).getOrThrow()

		val person =
			Person(
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

	fun hentAdressebeskyttelse(personident: String): AdressebeskyttelseGradering? = pdlClient.hentAdressebeskyttelse(personident)

	fun hentAlleMedRolle(
		offset: Int,
		limit: Int = 500,
		rolle: Rolle,
	) = repository
		.getAllWithRolle(offset, limit, rolle)
		.map { it.toModel() }
}
