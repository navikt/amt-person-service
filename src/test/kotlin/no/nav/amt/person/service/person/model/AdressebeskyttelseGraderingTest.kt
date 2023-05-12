package no.nav.amt.person.service.person.model

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class AdressebeskyttelseGraderingTest {
	@Test
	fun `erBeskyttet - er null eller ugradert - skal returnere false`() {
		val nullGradering: AdressebeskyttelseGradering? = null
		val ugradert = AdressebeskyttelseGradering.UGRADERT

		nullGradering.erBeskyttet() shouldBe false
		ugradert.erBeskyttet() shouldBe false
	}

	@Test
	fun `erBeskyttet - er ikke null eller ugradert - skal returnere true`() {
		val fortrolig = AdressebeskyttelseGradering.FORTROLIG
		val strengtFortrolig = AdressebeskyttelseGradering.STRENGT_FORTROLIG
		val strengtFortroligUtland = AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND

		fortrolig.erBeskyttet() shouldBe true
		strengtFortrolig.erBeskyttet() shouldBe true
		strengtFortroligUtland.erBeskyttet() shouldBe true
	}
}
