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
import org.junit.jupiter.api.Nested
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

    @Nested
    inner class HentEllerOpprettPerson {
        @Test
        fun `personen finnes ikke - opprettes fra PDL`() {
            // Arrange
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

            // Act
            val person = service.hentEllerOpprettPerson(personident)

            // Assert
            assertSoftly(person) {
                this.personident shouldBe personident
                fornavn shouldBe pdlPerson.fornavn
                mellomnavn shouldBe pdlPerson.mellomnavn
                etternavn shouldBe pdlPerson.etternavn
            }
        }

        @Test
        fun `personen finnes - returnerer eksisterende uten PDL-oppslag`() {
            // Arrange
            val personident = TestData.randomIdent()
            val eksisterendePerson = TestData.lagPerson(personident = personident)

            every { personRepository.get(personident) } returns eksisterendePerson

            // Act
            val person = service.hentEllerOpprettPerson(personident)

            // Assert
            person shouldBe eksisterendePerson
            verify(exactly = 0) { pdlClient.hentPerson(personident) }
        }

        @Test
        fun `forceFetchFromPdl - oppdaterer eksisterende person`() {
            // Arrange
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

            // Act
            val oppdatertPerson = service.hentEllerOpprettPerson(gammelPersonident, forceFetchFromPdl = true)

            // Assert
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
    }

    @Nested
    inner class HentEllerOpprettPersonMedPdlPerson {
        @Test
        fun `returnerer eksisterende person uten oppdatering`() {
            // Arrange
            val personident = TestData.randomIdent()
            val eksisterendePerson = TestData.lagPerson(
                personident = personident,
                fornavn = "Eksisterende",
                etternavn = "Person",
            )
            val pdlPerson = PdlPerson(
                erFalskIdentitet = false,
                fornavn = "Annet fornavn",
                mellomnavn = null,
                etternavn = "Annet etternavn",
                telefonnummer = null,
                adressebeskyttelseGradering = null,
                identer = listOf(
                    Personident(ident = personident, historisk = false, type = IdentType.FOLKEREGISTERIDENT),
                ),
                adresse = null,
            )

            every { personRepository.get(personident) } returns eksisterendePerson

            // Act
            val person = service.hentEllerOpprettPerson(personident, pdlPerson)

            // Assert
            person shouldBe eksisterendePerson

            verify(exactly = 0) { pdlClient.hentPerson(any()) }
            verify(exactly = 0) { personRepository.upsert(any()) }
        }
    }

    @Nested
    inner class OppdaterPersonIdent {
        @Test
        fun `flere personer knyttet til samme ident - kaster exception`() {
            // Arrange
            val identer = listOf(
                Personident(TestData.randomIdent(), false, IdentType.FOLKEREGISTERIDENT),
                Personident(TestData.randomIdent(), true, IdentType.FOLKEREGISTERIDENT),
                Personident(TestData.randomIdent(), true, IdentType.FOLKEREGISTERIDENT),
            )

            every { personRepository.getPersoner(identer.map { it.ident }.toSet()) } returns
                identer.map { TestData.lagPerson(personident = it.ident) }

            // Act & Assert
            shouldThrow<IllegalStateException> {
                service.oppdaterPersonIdent(identer)
            }
        }
    }
}
