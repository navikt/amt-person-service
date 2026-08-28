package no.nav.amt.person.service.person

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.amt.person.service.clients.pdl.PdlClient
import no.nav.amt.person.service.clients.pdl.PdlPerson
import no.nav.amt.person.service.data.TestData
import no.nav.amt.person.service.person.model.IdentType
import no.nav.amt.person.service.person.model.Personident
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationEventPublisher

class PersonServiceTest {
    private val pdlClient: PdlClient = mockk(relaxUnitFun = true)
    private val personRepository: PersonRepository = mockk(relaxUnitFun = true)
    private val personidentRepository: PersonidentRepository = mockk(relaxUnitFun = true)
    private val applicationEventPublisher: ApplicationEventPublisher = mockk(relaxUnitFun = true)

    private val service = PersonService(
        pdlClient = pdlClient,
        personRepository = personRepository,
        personidentRepository = personidentRepository,
        applicationEventPublisher = applicationEventPublisher,
    )

    @BeforeEach
    fun setup() = clearAllMocks()

    @Test
    fun `hentEllerOpprettPerson - personen finnes ikke - opprettes og returnere person`() {
        val personident = TestData.randomIdent()
        val identType = IdentType.FOLKEREGISTERIDENT
        val pdlPerson = PdlPerson(
            erFalskIdentitet = false,
            fornavn = "Fornavn",
            mellomnavn = "Mellomnavn",
            etternavn = "Etternavn",
            telefonnummer = "81549300",
            adressebeskyttelseGradering = null,
            identer = listOf(
                Personident(ident = personident, historisk = false, type = identType),
                Personident(ident = TestData.randomIdent(), historisk = true, type = identType),
            ),
            adresse = null,
        )

        every { pdlClient.hentPerson(personident) } returns pdlPerson
        every { personRepository.get(personident) } returns null

        val person = service.hentEllerOpprettPerson(personident)
        assertSoftly(person) {
            this.personident shouldBe personident
            fornavn shouldBe pdlPerson.fornavn
            mellomnavn shouldBe pdlPerson.mellomnavn
            etternavn shouldBe pdlPerson.etternavn
        }
    }

    @Test
    fun `hentEllerOpprettPerson - forceFetchFromPdl - oppdaterer eksisterende person`() {
        val gammelPersonident = TestData.randomIdent()
        val nyPersonident = TestData.randomIdent()
        val eksisterendePerson = TestData.lagPerson(
            personident = gammelPersonident,
            fornavn = "Gammelt",
            etternavn = "Navn",
            erFalskIdentitet = false,
        )
        val pdlPerson = PdlPerson(
            erFalskIdentitet = true,
            fornavn = "Nytt fornavn",
            mellomnavn = "Nytt mellomnavn",
            etternavn = "Nytt etternavn",
            telefonnummer = null,
            adressebeskyttelseGradering = null,
            identer = listOf(
                Personident(ident = nyPersonident, historisk = false, type = IdentType.FOLKEREGISTERIDENT),
                Personident(ident = gammelPersonident, historisk = true, type = IdentType.FOLKEREGISTERIDENT),
            ),
            adresse = null,
        )

        every { pdlClient.hentPerson(gammelPersonident) } returns pdlPerson
        every { personRepository.get(gammelPersonident) } returns eksisterendePerson

        val oppdatertPerson = service.hentEllerOpprettPerson(gammelPersonident, forceFetchFromPdl = true)

        assertSoftly(oppdatertPerson) {
            id shouldBe eksisterendePerson.id
            this.personident shouldBe nyPersonident
            erFalskIdentitet shouldBe true
            fornavn shouldBe "Nytt Fornavn"
            mellomnavn shouldBe "Nytt Mellomnavn"
            etternavn shouldBe "Nytt Etternavn"
        }

        verify(exactly = 1) { pdlClient.hentPerson(gammelPersonident) }
        verify {
            personidentRepository.upsert(
                match { identer ->
                    identer.size == 2 &&
                        identer.any { it.ident == nyPersonident && !it.historisk && it.personId == eksisterendePerson.id } &&
                        identer.any { it.ident == gammelPersonident && it.historisk && it.personId == eksisterendePerson.id }
                },
            )
        }
        verify { personRepository.upsert(oppdatertPerson) }
    }

    @Test
    fun `hentEllerOpprettPerson - person har ekte navn - bruker eksisterende uten å hente PDL`() {
        val personident = TestData.randomIdent()
        val eksisterendePerson = TestData.lagPerson(personident = personident)

        every { personRepository.get(personident) } returns eksisterendePerson

        val person = service.hentEllerOpprettPerson(personident)

        person shouldBe eksisterendePerson
        verify(exactly = 0) { pdlClient.hentPerson(personident) }
    }

    @Test
    fun `oppdaterPersonIdent - flere personer knyttet til samme ident - kaster exception`() {
        val identer = listOf(
            Personident(TestData.randomIdent(), false, IdentType.FOLKEREGISTERIDENT),
            Personident(TestData.randomIdent(), true, IdentType.FOLKEREGISTERIDENT),
            Personident(TestData.randomIdent(), true, IdentType.FOLKEREGISTERIDENT),
        )

        every { personRepository.getPersoner(identer.map { it.ident }.toSet()) } returns
            identer.map { TestData.lagPerson(personident = it.ident) }

        shouldThrow<IllegalStateException> {
            service.oppdaterPersonIdent(identer)
        }
    }
}
