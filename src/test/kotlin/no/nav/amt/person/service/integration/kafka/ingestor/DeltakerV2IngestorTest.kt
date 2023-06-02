package no.nav.amt.person.service.integration.kafka.ingestor

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.amt.person.service.clients.amt_tiltak.BrukerInfoDto
import no.nav.amt.person.service.data.TestData
import no.nav.amt.person.service.integration.IntegrationTestBase
import no.nav.amt.person.service.integration.kafka.utils.KafkaMessageSender
import no.nav.amt.person.service.integration.mock.servers.MockKontaktinformasjon
import no.nav.amt.person.service.kafka.ingestor.DeltakerDto
import no.nav.amt.person.service.migrering.MigreringService
import no.nav.amt.person.service.nav_bruker.NavBrukerService
import no.nav.amt.person.service.utils.AsyncUtils
import no.nav.amt.person.service.utils.JsonUtils
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

class DeltakerV2IngestorTest : IntegrationTestBase() {

	@Autowired
	lateinit var kafkaMessageSender: KafkaMessageSender

	@Autowired
	lateinit var navBrukerService: NavBrukerService

	@Autowired
	lateinit var migreringService: MigreringService

	@Test
	fun `ingest - bruker finnes ikke - oppretter med riktig personId`() {
		val navBruker = TestData.lagNavBruker()
		val deltakerId = UUID.randomUUID()
		val kontaktinfoDiff = MockKontaktinformasjon("ny@epost", "4277742")

		testDataRepository.insertNavAnsatt(navBruker.navVeileder!!)
		testDataRepository.insertNavEnhet(navBruker.navEnhet!!)

		val msg = DeltakerDto(
			id = deltakerId,
			personalia = DeltakerDto.DeltakerPersonaliaDto(
				navBruker.person.personIdent,
				DeltakerDto.DeltakerPersonaliaDto.NavnDto(
					navBruker.person.fornavn,
					navBruker.person.mellomnavn,
					navBruker.person.etternavn,
				),
				DeltakerDto.DeltakerPersonaliaDto.DeltakerKontaktinformasjonDto(
					navBruker.telefon,
					navBruker.epost,
				),
				navBruker.erSkjermet,
			),
			navVeileder = DeltakerDto.DeltakerNavVeilederDto(
				id = navBruker.navVeileder!!.id,
				navn = navBruker.navVeileder!!.navn,
				epost = navBruker.navVeileder!!.epost,
			),
		)

		mockAmtTiltakHttpServer.mockHentBrukerInfo(deltakerId, BrukerInfoDto(
			navBruker.person.id,
			personIdentType = navBruker.person.personIdentType!!,
			historiskeIdenter = navBruker.person.historiskeIdenter,
			navEnhetId = navBruker.navEnhet!!.id
		))

		mockPdlHttpServer.mockHentPerson(navBruker.person)
		mockKrrProxyHttpServer.mockHentKontaktinformasjon(kontaktinfoDiff)
		mockPoaoTilgangHttpServer.addErSkjermetResponse(mapOf(navBruker.person.personIdent to navBruker.erSkjermet))
		mockVeilarboppfolgingHttpServer.mockHentVeilederIdent(navBruker.person.personIdent, navBruker.navVeileder!!.navIdent)
		mockVeilarbarenaHttpServer.mockHentBrukerOppfolgingsenhetId(navBruker.person.personIdent, navBruker.navEnhet!!.enhetId)

		kafkaMessageSender.sendTilDeltakerV2Topic(deltakerId, JsonUtils.toJsonString(msg))

		AsyncUtils.eventually {
			val faktiskBruker = navBrukerService.hentNavBruker(navBruker.person.personIdent)
			faktiskBruker shouldNotBe null

			faktiskBruker!!.navEnhet?.id shouldBe navBruker.navEnhet?.id
			faktiskBruker.navVeileder?.id shouldBe navBruker.navVeileder?.id

			faktiskBruker.telefon shouldBe kontaktinfoDiff.mobiltelefonnummer
			faktiskBruker.epost shouldBe kontaktinfoDiff.epostadresse

			val migrering = migreringService.hentMigrering(navBruker.person.id)
			migrering shouldNotBe null

			migrering!!.diff shouldBe """{"epost": {"amtPerson": "ny@epost", "amtTiltak": "nav_bruker@gmail.com"}, "telefon": {"amtPerson": "4277742", "amtTiltak": "77788999"}}"""

			migrering.error shouldBe null
			migrering.endepunkt shouldBe "nav-bruker"
		}
	}

}
