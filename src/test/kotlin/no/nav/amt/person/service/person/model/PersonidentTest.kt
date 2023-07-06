package no.nav.amt.person.service.person.model

import io.kotest.matchers.shouldBe
import no.nav.amt.person.service.data.TestData
import org.junit.jupiter.api.Test

class PersonidentTest {
	@Test
	fun `finnGjeldendeIdent - flere typer gjeldende identer - fregident er gjeldende`() {
		val forventetIdent = TestData.lagPersonident(historisk = false, type = IdentType.FOLKEREGISTERIDENT)
		val identer = listOf(
			forventetIdent,
			TestData.lagPersonident(historisk = true, type = IdentType.FOLKEREGISTERIDENT),
			TestData.lagPersonident(historisk = false, type = IdentType.NPID),
			TestData.lagPersonident(historisk = false, type = IdentType.AKTORID),
		)

		finnGjeldendeIdent(identer).getOrThrow() shouldBe forventetIdent
	}

	@Test
	fun `finnGjeldendeIdent - ingen fregident - npid er gjeldende`() {
		val forventetIdent = TestData.lagPersonident(historisk = false, type = IdentType.NPID)
		val identer = listOf(
			TestData.lagPersonident(historisk = false, type = IdentType.AKTORID),
			forventetIdent,
		)

		finnGjeldendeIdent(identer).getOrThrow() shouldBe forventetIdent
	}

	@Test
	fun `finnGjeldendeIdent - kun aktorid - returner failure`() {
		val identer = listOf(
			TestData.lagPersonident(historisk = false, type = IdentType.AKTORID),
		)

		finnGjeldendeIdent(identer).isFailure shouldBe true
	}
}
