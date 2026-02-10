package no.nav.amt.person.service.person

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.amt.person.service.clients.pdl.PdlClient
import no.nav.amt.person.service.clients.pdl.PdlPerson
import no.nav.amt.person.service.data.TestData
import no.nav.amt.person.service.person.model.IdentType
import no.nav.amt.person.service.person.model.Personident
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.context.ApplicationEventPublisher

class PersonServiceTest {
	private val pdlClient: PdlClient = mockk(relaxUnitFun = true)
	private val personRepository: PersonRepository = mockk(relaxUnitFun = true)
	private val personidentRepository: PersonidentRepository = mockk(relaxUnitFun = true)
	private val applicationEventPublisher: ApplicationEventPublisher = mockk(relaxUnitFun = true)

	private val service =
		PersonService(
			pdlClient = pdlClient,
			repository = personRepository,
			personidentRepository = personidentRepository,
			applicationEventPublisher = applicationEventPublisher,
		)

	@BeforeEach
	fun setup() = clearAllMocks()

	@Test
	fun `hentEllerOpprettPerson - personen finnes ikke - opprettes og returnere person`() {
		val personident = TestData.randomIdent()
		val identType = IdentType.FOLKEREGISTERIDENT
		val pdlPerson =
			PdlPerson(
				fornavn = "Fornavn",
				mellomnavn = "Mellomnavn",
				etternavn = "Etternavn",
				telefonnummer = "81549300",
				adressebeskyttelseGradering = null,
				identer =
					listOf(
						Personident(ident = personident, historisk = false, type = identType),
						Personident(ident = TestData.randomIdent(), historisk = true, type = identType),
					),
				adresse = null,
			)

		every { pdlClient.hentPerson(personident) } returns pdlPerson
		every { personRepository.get(personident) } returns null

		val person = service.hentEllerOpprettPerson(personident)
		assertSoftly(person) {
			personident shouldBe personident
			fornavn shouldBe pdlPerson.fornavn
			mellomnavn shouldBe pdlPerson.mellomnavn
			etternavn shouldBe pdlPerson.etternavn
		}
	}

	@Test
	fun `oppdaterPersonIdent - flere personer knyttet til samme ident - kaster exception`() {
		val identer =
			listOf(
				Personident(TestData.randomIdent(), false, IdentType.FOLKEREGISTERIDENT),
				Personident(TestData.randomIdent(), true, IdentType.FOLKEREGISTERIDENT),
				Personident(TestData.randomIdent(), true, IdentType.FOLKEREGISTERIDENT),
			)

		every { personRepository.getPersoner(identer.map { it.ident }) } returns
			identer.map { TestData.lagPerson(personident = it.ident) }

		assertThrows<IllegalStateException> {
			service.oppdaterPersonIdent(identer)
		}
	}
}
