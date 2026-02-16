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
	private val pdlClient: PdlClient,
	private val personRepository: PersonRepository,
	private val personidentRepository: PersonidentRepository,
	private val applicationEventPublisher: ApplicationEventPublisher,
) {
	private val log = LoggerFactory.getLogger(javaClass)

	@Retryable(maxRetries = 2)
	@Transactional
	fun hentEllerOpprettPerson(personident: String): Person =
		personRepository.get(personident)?.toModel() ?: run {
			val pdlPerson = pdlClient.hentPerson(personident)
			check(!pdlPerson.erUkjent()) { "Person har ukjent etternavn, oppretter ikke person" }
			opprettPerson(pdlPerson)
		}

	@Transactional
	fun hentEllerOpprettPerson(
		personident: String,
		pdlPerson: PdlPerson,
	): Person = personRepository.get(personident)?.toModel() ?: opprettPerson(pdlPerson)

	@Transactional
	fun oppdaterPersonIdent(identer: List<Personident>) {
		val personer = personRepository.getPersoner(identer.map { it.ident }.toSet())

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
		if (person.erUkjent()) {
			log.info("Skipper oppdaterNavn for ${person.id} med ukjent etternavn")
			return
		}

		val pdlPerson =
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
			person.fornavn == pdlPerson.fornavn &&
			person.mellomnavn == pdlPerson.mellomnavn &&
			person.etternavn == pdlPerson.etternavn
		) {
			log.info("Navn på person ${person.id} er allerede oppdatert, ingen endringer gjort.")
			return
		}

		upsert(
			person.copy(
				fornavn = pdlPerson.fornavn,
				mellomnavn = pdlPerson.mellomnavn,
				etternavn = pdlPerson.etternavn,
			),
		)

		log.info("Oppdaterte navn på person ${person.id}")
	}

	fun upsert(person: Person) {
		personRepository.upsert(person)
		applicationEventPublisher.publishEvent(PersonUpdateEvent(person))

		log.info("Upsertet person med id: ${person.id}")
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
	) = personRepository
		.getAllWithRolle(offset, limit, rolle)
		.map { it.toModel() }
}
