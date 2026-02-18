package no.nav.amt.person.service.person.dbo

import io.kotest.matchers.shouldBe
import no.nav.amt.person.service.data.TestData.lagPerson
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class PersonDboTest {
	@ParameterizedTest
	@ValueSource(
		strings = [
			"Ukjent",
			"ukjent",
			"UKJENT",
			"uKjEnT",
			"UkJeNt",
		],
	)
	fun `erUkjent skal returnere true uavhengig av casing for order Ukjent`(etternavn: String) {
		val person = lagPerson(etternavn = etternavn)

		person.erUkjent() shouldBe true
	}
}
