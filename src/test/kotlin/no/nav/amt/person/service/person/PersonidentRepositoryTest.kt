package no.nav.amt.person.service.person

import io.kotest.matchers.shouldBe
import no.nav.amt.person.service.data.RepositoryTestBase
import no.nav.amt.person.service.data.TestData
import no.nav.amt.person.service.person.model.IdentType
import no.nav.amt.person.service.person.model.Personident
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(classes = [PersonidentRepository::class])
class PersonidentRepositoryTest(
	private val personidentRepository: PersonidentRepository,
) : RepositoryTestBase() {
	@Test
	fun `upsertPersonident - nye identer - inserter`() {
		val person = TestData.lagPerson()
		testDataRepository.insertPerson(person)

		val identer =
			listOf(
				Personident(TestData.randomIdent(), true, IdentType.AKTORID),
				Personident(TestData.randomIdent(), true, IdentType.NPID),
			)

		personidentRepository.upsert(person.id, identer)

		val faktiskeIdenter = personidentRepository.getAllForPerson(person.id)

		faktiskeIdenter.any { it.ident == identer[0].ident } shouldBe true
		faktiskeIdenter.any { it.ident == identer[1].ident } shouldBe true
	}

	@Test
	fun `upsertPersonident - ny ident - inserter og oppdaterer`() {
		val person = TestData.lagPerson()
		testDataRepository.insertPerson(person)

		val identer =
			listOf(
				TestData.lagPersonident(person.personident, historisk = true),
				TestData.lagPersonident(TestData.randomIdent(), historisk = false),
			)

		personidentRepository.upsert(person.id, identer.map { it.toModel() })

		val faktiskeIdenter = personidentRepository.getAllForPerson(person.id)

		faktiskeIdenter.any { it.ident == identer[0].ident && it.historisk } shouldBe true
		faktiskeIdenter.any { it.ident == identer[1].ident && !it.historisk } shouldBe true
	}
}
