package no.nav.amt.person.service.nav_ansatt

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.amt.person.service.clients.nom.NomClientImpl
import no.nav.amt.person.service.clients.nom.NomNavAnsatt
import no.nav.amt.person.service.clients.veilarboppfolging.VeilarboppfolgingClient
import no.nav.amt.person.service.data.TestData
import no.nav.amt.person.service.kafka.producer.KafkaProducerService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class NavAnsattServiceTest {
	lateinit var navAnsattRepository: NavAnsattRepository
	lateinit var nomClient: NomClientImpl
	lateinit var veilarboppfolgingClient: VeilarboppfolgingClient
	lateinit var service: NavAnsattService
	lateinit var kafkaProducerService: KafkaProducerService

	@BeforeEach
	fun setup() {
		navAnsattRepository = mockk(relaxUnitFun = true)
		nomClient = mockk()
		veilarboppfolgingClient = mockk()
		kafkaProducerService = mockk(relaxUnitFun = true)
		service = NavAnsattService(
			navAnsattRepository = navAnsattRepository,
			nomClient = nomClient,
			veilarboppfolgingClient = veilarboppfolgingClient,
			kafkaProducerService = kafkaProducerService,
		)
	}

	@Test
	fun `hentHellerOpprettAnsatt - ansatt finnes ikke - oppretter og returnerer ansatt`() {
		val ansatt = TestData.lagNavAnsatt()

		every { navAnsattRepository.get(ansatt.navIdent) } returns null
		every { nomClient.hentNavAnsatt(ansatt.navIdent) } returns NomNavAnsatt(
			navIdent = ansatt.navIdent,
			navn = ansatt.navn,
			telefonnummer = ansatt.telefon,
			epost = ansatt.epost,
		)
		every { navAnsattRepository.upsert(any()) } returns ansatt

		val faktiskAnsatt = service.hentEllerOpprettAnsatt(ansatt.navIdent)

		faktiskAnsatt.navIdent shouldBe ansatt.navIdent
		faktiskAnsatt.navn shouldBe ansatt.navn
		faktiskAnsatt.telefon shouldBe ansatt.telefon
		faktiskAnsatt.epost shouldBe ansatt.epost
	}

}
