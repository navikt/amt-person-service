package no.nav.amt.person.service.person

import no.nav.amt.person.service.clients.pdl.PdlClient
import no.nav.amt.person.service.clients.pdl.PdlPerson
import no.nav.amt.person.service.person.dbo.PersonDbo
import no.nav.amt.person.service.person.model.Personident
import no.nav.amt.person.service.person.model.Personident.Companion.finnGjeldendeIdent
import no.nav.amt.person.service.utils.EnvUtils
import no.nav.amt.person.service.utils.titlecase
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.resilience.annotation.Retryable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PersonService(
    private val pdlClient: PdlClient,
    private val personRepository: PersonRepository,
    private val personidentRepository: PersonidentRepository,
    private val applicationEventPublisher: ApplicationEventPublisher,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Henter eksisterende person eller oppretter ny fra PDL.
     * Med [forceFetchFromPdl] oppdateres eksisterende person med ferske data fra PDL.
     *
     * @param personident fødselsnummer eller d-nummer
     * @param forceFetchFromPdl hvis true, hentes alltid ferske data fra PDL og eksisterende person oppdateres
     * @return eksisterende, oppdatert eller nyopprettet person
     */
    @Retryable(maxRetries = 2)
    @Transactional
    fun hentEllerOpprettPerson(
        personident: String,
        forceFetchFromPdl: Boolean = false,
    ): PersonDbo {
        val eksisterendePerson = personRepository.get(personident)

        if (!(forceFetchFromPdl || eksisterendePerson == null)) return eksisterendePerson

        val pdlPerson = pdlClient.hentPerson(personident)

        return eksisterendePerson
            ?.let {
                oppdaterPersonFraPdl(
                    person = it,
                    pdlPerson = pdlPerson,
                )
            }
            ?: opprettPerson(pdlPerson)
    }

    /**
     * Oppdaterer eksisterende person med gitt [pdlPerson], eller oppretter ny.
     * Gjør ikke eget PDL-oppslag — bruk når kalleren allerede har hentet PdlPerson.
     *
     * @param personident fødselsnummer eller d-nummer
     * @param pdlPerson ferske persondata fra PDL
     * @return oppdatert eller nyopprettet person
     */
    @Transactional
    fun hentEllerOpprettPerson(
        personident: String,
        pdlPerson: PdlPerson,
    ): PersonDbo = personRepository
        .get(personident)
        ?.let {
            oppdaterPersonFraPdl(
                person = it,
                pdlPerson = pdlPerson,
            )
        }
        ?: opprettPerson(pdlPerson)

    @Transactional
    fun oppdaterPersonIdent(identer: List<Personident>) {
        val personer = personRepository.getPersoner(identer.map { it.ident }.toSet())

        if (personer.size > 1) {
            log.error("Vi har flere personer knyttet til samme identer: ${personer.joinToString { it.id.toString() }}")
            throw IllegalStateException("Vi har flere personer knyttet til samme identer")
        }

        val gjeldendeIdent = identer.finnGjeldendeIdent().getOrThrow()

        personer.firstOrNull()?.let { person ->
            log.info("Oppdaterer personident for person ${person.id}")
            personidentRepository.upsert(identer.map { it.toDbo(person.id) }.toSet())
            upsert(person.copy(personident = gjeldendeIdent.ident))
        }
    }

    @Transactional
    fun oppdaterNavn(person: PersonDbo) {
        val pdlPerson = try {
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
                fornavn = pdlPerson.fornavn.titlecase(),
                mellomnavn = pdlPerson.mellomnavn?.titlecase(),
                etternavn = pdlPerson.etternavn.titlecase(),
            ),
        )

        log.info("Oppdaterte navn på person ${person.id}")
    }

    fun upsert(person: PersonDbo) {
        personRepository.upsert(person)
        applicationEventPublisher.publishEvent(PersonUpdateEvent(person))

        log.info("Upsertet person med id: ${person.id}")
    }

    private fun opprettPerson(pdlPerson: PdlPerson): PersonDbo {
        val person = pdlPerson.toPersonDbo()
        upsert(person)

        personidentRepository.upsert(
            identer = pdlPerson.identer.map { it.toDbo(person.id) }.toSet(),
        )

        log.info("Opprettet ny person med id ${person.id}")

        return person
    }

    private fun oppdaterPersonFraPdl(
        person: PersonDbo,
        pdlPerson: PdlPerson,
    ): PersonDbo {
        personidentRepository.upsert(
            identer = pdlPerson.identer.map { it.toDbo(person.id) }.toSet(),
        )

        val oppdatertPerson = person.copy(
            erFalskIdentitet = pdlPerson.erFalskIdentitet,
            fornavn = pdlPerson.fornavn.titlecase(),
            mellomnavn = pdlPerson.mellomnavn?.titlecase(),
            etternavn = pdlPerson.etternavn.titlecase(),
            personident = pdlPerson.identer
                .finnGjeldendeIdent()
                .getOrThrow()
                .ident,
        )

        upsert(oppdatertPerson)
        return oppdatertPerson
    }
}
