package no.nav.amt.person.service.nav_ansatt

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.amt.person.service.clients.nom.NomClient
import no.nav.amt.person.service.clients.nom.NomNavAnsatt
import no.nav.amt.person.service.data.TestData
import no.nav.amt.person.service.utils.LogUtils
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class NavAnsattUpdaterTest {
	lateinit var repository: NavAnsattRepository
	lateinit var nomClient: NomClient
	lateinit var updater: NavAnsattUpdater

	@BeforeEach
	fun setup() {
		repository = mockk(relaxUnitFun = true)
		nomClient = mockk()

		updater = NavAnsattUpdater(repository, nomClient)
	}

	@Test
	fun `oppdaterAlle - navIdent mangler hos Nom - logger warning`() {
		val ansatt1 = TestData.lagNavAnsatt()
		val ansatt2 = TestData.lagNavAnsatt()

		every { repository.getAll() } returns listOf(ansatt1, ansatt2)
		every { nomClient.hentNavAnsatte(listOf(ansatt1.navIdent, ansatt2.navIdent)) } returns listOf(
			NomNavAnsatt(
				navIdent = ansatt1.navIdent,
				navn = ansatt1.navn,
				telefonnummer = ansatt1.telefon,
				epost = ansatt1.epost,
			)
		)

		LogUtils.withLogs { getLogs ->
			updater.oppdaterAlle()

			getLogs().any {
				it.message == "Fant ikke nav ansatt med ident=${ansatt1.navIdent} id=${ansatt1.id} i NOM"
			} shouldBe false

			getLogs().any {
				it.message == "Fant ikke nav ansatt med ident=${ansatt2.navIdent} id=${ansatt2.id} i NOM"
			} shouldBe true

		}

		verify(exactly = 1) { repository.upsertMany(listOf(ansatt1.toModel())) }
	}

}
