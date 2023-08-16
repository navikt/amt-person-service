package no.nav.amt.person.service.person

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.amt.person.service.clients.pdl.PdlClient
import no.nav.amt.person.service.clients.pdl.PdlPerson
import no.nav.amt.person.service.data.TestData
import no.nav.amt.person.service.person.model.IdentType
import no.nav.amt.person.service.person.model.Personident
import no.nav.amt.person.service.utils.mockExecuteWithoutResult
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.context.ApplicationEventPublisher
import org.springframework.transaction.support.TransactionTemplate

class PersonServiceTest {

	lateinit var pdlClient: PdlClient
	lateinit var repository: PersonRepository
	lateinit var personidentRepository: PersonidentRepository
	lateinit var service: PersonService
	lateinit var applicationEventPublisher: ApplicationEventPublisher
	lateinit var transactionTemplate: TransactionTemplate

	@BeforeEach
	fun setup() {
		pdlClient = mockk(relaxUnitFun = true)
		repository = mockk(relaxUnitFun = true)
		personidentRepository = mockk(relaxUnitFun = true)
		applicationEventPublisher = mockk(relaxUnitFun = true)
		transactionTemplate = mockk()
		service = PersonService(
			pdlClient = pdlClient,
			repository = repository,
			personidentRepository = personidentRepository,
			applicationEventPublisher = applicationEventPublisher,
			transactionTemplate = transactionTemplate
		)
	}

	@Test
	fun `hentEllerOpprettPerson - personen finnes ikke - opprettes og returnere person`() {
		val personident = TestData.randomIdent()
		val identType = IdentType.FOLKEREGISTERIDENT
		val pdlPerson = PdlPerson(
			fornavn = "Fornavn",
			mellomnavn = "Mellomnavn",
			etternavn = "Etternavn",
			telefonnummer = "81549300",
			adressebeskyttelseGradering = null,
			identer = listOf(
				Personident(ident = personident, historisk = false, type = identType),
				Personident(ident = TestData.randomIdent(), historisk = true, type = identType),
			),
			adresse = null
		)

		every { pdlClient.hentPerson(personident) } returns pdlPerson
		every { repository.get(personident) } returns null
		mockExecuteWithoutResult(transactionTemplate)

		val person = service.hentEllerOpprettPerson(personident)
		person.personident shouldBe personident
		person.fornavn shouldBe pdlPerson.fornavn
		person.mellomnavn shouldBe pdlPerson.mellomnavn
		person.etternavn shouldBe pdlPerson.etternavn
	}

	@Test
	fun `oppdaterPersonIdent - flere personer knyttet til samme ident - kaster exception`() {
		val identer = listOf(
			Personident(TestData.randomIdent(), false, IdentType.FOLKEREGISTERIDENT),
			Personident(TestData.randomIdent(), true, IdentType.FOLKEREGISTERIDENT),
			Personident(TestData.randomIdent(), true, IdentType.FOLKEREGISTERIDENT),
		)

		every { repository.getPersoner(identer.map { it.ident }) } returns
			identer.map { TestData.lagPerson(personident = it.ident) }
		mockExecuteWithoutResult(transactionTemplate)

		assertThrows<IllegalStateException> {
			service.oppdaterPersonIdent(identer)
		}

	}
}
