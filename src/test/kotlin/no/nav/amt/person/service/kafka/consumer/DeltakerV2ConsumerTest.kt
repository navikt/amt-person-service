package no.nav.amt.person.service.kafka.consumer

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.amt.person.service.data.TestData
import no.nav.amt.person.service.kafka.consumer.dto.DeltakerDto
import no.nav.amt.person.service.kafka.consumer.dto.DeltakerPersonaliaDto
import no.nav.amt.person.service.nav_bruker.NavBrukerService
import no.nav.amt.person.service.utils.JsonUtils.objectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class DeltakerV2ConsumerTest {
	private val navBrukerService = mockk<NavBrukerService>(relaxed = true)
	private val deltakerV2Consumer = DeltakerV2Consumer(navBrukerService)

	@BeforeEach
	fun setup() {
		clearMocks(navBrukerService)
	}

	@Test
	fun `ingest - sist synkronisert med krr for 4 uker siden - oppdaterer kontaktinformasjon`() {
		val personident = "12345678910"
		val bruker = TestData.lagNavBruker().copy(sisteKrrSync = LocalDateTime.now().minusWeeks(4)).toModel()
		every { navBrukerService.hentNavBruker(personident) } returns bruker

		deltakerV2Consumer.ingest(
			objectMapper.writeValueAsString(DeltakerDto(
				id = UUID.randomUUID(),
				personalia = DeltakerPersonaliaDto(
					personident = personident
				)
			))
		)

		verify(exactly = 1) { navBrukerService.oppdaterKontaktinformasjon(bruker) }
	}

	@Test
	fun `ingest - sist synkronisert med krr for 4 dager siden - oppdaterer ikke kontaktinformasjon`() {
		val personident = "12345678910"
		val bruker = TestData.lagNavBruker().copy(sisteKrrSync = LocalDateTime.now().minusDays(4)).toModel()
		every { navBrukerService.hentNavBruker(personident) } returns bruker

		deltakerV2Consumer.ingest(
			objectMapper.writeValueAsString(DeltakerDto(
				id = UUID.randomUUID(),
				personalia = DeltakerPersonaliaDto(
					personident = personident
				)
			))
		)

		verify(exactly = 0) { navBrukerService.oppdaterKontaktinformasjon(any()) }
	}
}
