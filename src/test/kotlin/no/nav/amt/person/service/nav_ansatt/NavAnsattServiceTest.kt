package no.nav.amt.person.service.nav_ansatt

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.amt.person.service.clients.nom.NomClientImpl
import no.nav.amt.person.service.clients.nom.NomNavAnsatt
import no.nav.amt.person.service.clients.nom.NomQueries
import no.nav.amt.person.service.clients.veilarboppfolging.VeilarboppfolgingClient
import no.nav.amt.person.service.data.TestData
import no.nav.amt.person.service.kafka.producer.KafkaProducerService
import no.nav.amt.person.service.nav_enhet.NavEnhet
import no.nav.amt.person.service.nav_enhet.NavEnhetService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class NavAnsattServiceTest {
	private lateinit var navAnsattRepository: NavAnsattRepository
	private lateinit var nomClient: NomClientImpl
	private lateinit var veilarboppfolgingClient: VeilarboppfolgingClient
	private lateinit var service: NavAnsattService
	private lateinit var kafkaProducerService: KafkaProducerService
	private lateinit var navEnhetService: NavEnhetService

	@BeforeEach
	fun setup() {
		navAnsattRepository = mockk(relaxUnitFun = true)
		nomClient = mockk()
		veilarboppfolgingClient = mockk()
		kafkaProducerService = mockk(relaxUnitFun = true)
		navEnhetService = mockk()
		service = NavAnsattService(
			navAnsattRepository = navAnsattRepository,
			nomClient = nomClient,
			veilarboppfolgingClient = veilarboppfolgingClient,
			kafkaProducerService = kafkaProducerService,
			navEnhetService = navEnhetService,
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
			orgTilknytning = orgTilknytning,
		)
		every { navAnsattRepository.upsert(any()) } returns ansatt
		every { navEnhetService.hentEllerOpprettNavEnhet(any()) } returns navGrunerlokka

		val faktiskAnsatt = service.hentEllerOpprettAnsatt(ansatt.navIdent)

		faktiskAnsatt.navIdent shouldBe ansatt.navIdent
		faktiskAnsatt.navn shouldBe ansatt.navn
		faktiskAnsatt.telefon shouldBe ansatt.telefon
		faktiskAnsatt.epost shouldBe ansatt.epost
	}

}

val orgTilknytning = listOf(
	NomQueries.HentRessurser.OrgTilknytning(
	gyldigFom = LocalDate.of(2020, 1, 1),
	gyldigTom = null,
	orgEnhet = NomQueries.HentRessurser.OrgTilknytning.OrgEnhet("0315"),
	erDagligOppfolging = true,
	)
)

val navGrunerlokka = NavEnhet(
	id = UUID(0L, 0L),
	navn = "Nav Grünerløkka",
	enhetId = "0315",
)
