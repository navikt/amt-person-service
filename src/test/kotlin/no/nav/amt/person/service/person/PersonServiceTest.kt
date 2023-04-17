package no.nav.amt.person.service.person

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.amt.person.service.clients.pdl.PdlClient
import no.nav.amt.person.service.clients.pdl.PdlPerson
import no.nav.amt.person.service.clients.pdl.PdlPersonIdent
import no.nav.amt.person.service.data.TestData
import no.nav.amt.person.service.person.model.IdentType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PersonServiceTest {

	lateinit var pdlClient: PdlClient
	lateinit var repository: PersonRepository
	lateinit var service: PersonService

	@BeforeEach
	fun setup() {
		pdlClient = mockk(relaxUnitFun = true)
		repository = mockk(relaxUnitFun = true)
		service = PersonService(pdlClient, repository)
	}

	@Test
	fun `get(personIdent) - personen finnes ikke - opprettes og returnere person`() {
		val personIdent = TestData.randomIdent()
		val identType = IdentType.FOLKEREGISTERIDENT
		val pdlPerson = PdlPerson(
			fornavn = "Fornavn",
			mellomnavn = "Mellomnavn",
			etternavn = "Etternavn",
			telefonnummer = "81549300",
			adressebeskyttelseGradering = null,
			identer = listOf(
				PdlPersonIdent(ident = personIdent, historisk = false, gruppe = identType.toString()),
				PdlPersonIdent(ident = TestData.randomIdent(), historisk = true, gruppe = identType.toString()),
			)
		)

		every { pdlClient.hentPerson(personIdent) } returns pdlPerson
		every { repository.get(personIdent) } returns null

		val person = service.hentPerson(personIdent)
		person.personIdent shouldBe personIdent
		person.personIdentType shouldBe identType
		person.fornavn shouldBe pdlPerson.fornavn
		person.mellomnavn shouldBe pdlPerson.mellomnavn
		person.etternavn shouldBe pdlPerson.etternavn
		person.historiskeIdenter shouldHaveSize 1
	}
}
