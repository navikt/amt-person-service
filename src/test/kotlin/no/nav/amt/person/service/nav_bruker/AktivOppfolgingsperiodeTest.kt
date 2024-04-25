package no.nav.amt.person.service.nav_bruker

import io.kotest.matchers.shouldBe
import no.nav.amt.person.service.data.TestData
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class AktivOppfolgingsperiodeTest {
	@Test
	fun `harAktivOppfolgingsperiode - har ingen oppfolgingsperioder - returnerer false`() {
		harAktivOppfolgingsperiode(emptyList()) shouldBe false
	}

	@Test
	fun `harAktivOppfolgingsperiode - har ikke startet - returnerer false`() {
		val oppfolgingsperiode = TestData.lagOppfolgingsperiode(
			startdato = LocalDateTime.now().plusDays(2),
			sluttdato = null
		)

		harAktivOppfolgingsperiode(listOf(oppfolgingsperiode)) shouldBe false
	}

	@Test
	fun `harAktivOppfolgingsperiode - startdato passert, sluttdato null - returnerer true`() {
		val oppfolgingsperiode = TestData.lagOppfolgingsperiode(
			startdato = LocalDateTime.now().minusDays(2),
			sluttdato = null
		)

		harAktivOppfolgingsperiode(listOf(oppfolgingsperiode)) shouldBe true
	}

	@Test
	fun `harAktivOppfolgingsperiode - startdato passert, sluttdato om en uke - returnerer true`() {
		val oppfolgingsperiode = TestData.lagOppfolgingsperiode(
			startdato = LocalDateTime.now().minusDays(2),
			sluttdato = LocalDateTime.now().plusWeeks(1)
		)

		harAktivOppfolgingsperiode(listOf(oppfolgingsperiode)) shouldBe true
	}

	@Test
	fun `harAktivOppfolgingsperiode - startdato passert, sluttdato for 25 dager siden - returnerer true`() {
		val oppfolgingsperiode = TestData.lagOppfolgingsperiode(
			startdato = LocalDateTime.now().minusYears(1),
			sluttdato = LocalDateTime.now().minusDays(25)
		)

		harAktivOppfolgingsperiode(listOf(oppfolgingsperiode)) shouldBe true
	}

	@Test
	fun `harAktivOppfolgingsperiode - startdato passert, sluttdato for 29 dager siden - returnerer false`() {
		val oppfolgingsperiode = TestData.lagOppfolgingsperiode(
			startdato = LocalDateTime.now().minusYears(1),
			sluttdato = LocalDateTime.now().minusDays(29)
		)

		harAktivOppfolgingsperiode(listOf(oppfolgingsperiode)) shouldBe false
	}
}
