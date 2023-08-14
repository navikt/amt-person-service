package no.nav.amt.person.service.clients.kodeverk

import io.kotest.matchers.shouldBe
import no.nav.amt.person.service.utils.JsonUtils.objectMapper
import org.junit.jupiter.api.Test

class KodeverkResponseMappingTest {
	@Test
	fun `toPostInformasjonListe - reell respons fra kodeverk - kodeverkresponse mappes riktig`() {
		val kodeverkrespons = objectMapper.readValue(
			KodeverkResponseMappingTest::class.java.getResourceAsStream("/kodeverkrespons.json"),
			GetKodeverkKoderBetydningerResponse::class.java
		)

		val postinformasjonListe = kodeverkrespons.toPostInformasjonListe()

		postinformasjonListe.find { it.postnummer == "3831" }?.poststed shouldBe "ULEFOSS"
	}
}
