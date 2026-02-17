package no.nav.amt.person.service.navansatt

import no.nav.amt.person.service.clients.nom.NomClient
import no.nav.amt.person.service.clients.veilarboppfolging.VeilarboppfolgingClient
import no.nav.amt.person.service.kafka.producer.KafkaProducerService
import no.nav.amt.person.service.navenhet.NavEnhetService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class NavAnsattService(
	private val navAnsattRepository: NavAnsattRepository,
	private val nomClient: NomClient,
	private val veilarboppfolgingClient: VeilarboppfolgingClient,
	private val kafkaProducerService: KafkaProducerService,
	private val navEnhetService: NavEnhetService,
) {
	private val log = LoggerFactory.getLogger(javaClass)

	fun upsert(navAnsatt: NavAnsattDbo): NavAnsattDbo {
		val upsertedNavAnsatt = navAnsattRepository.upsert(navAnsatt)
		kafkaProducerService.publiserNavAnsatt(upsertedNavAnsatt)
		return upsertedNavAnsatt
	}

	fun upsertMany(ansatte: Set<NavAnsattDbo>) {
		navAnsattRepository.upsertMany(ansatte)
		ansatte.forEach { kafkaProducerService.publiserNavAnsatt(it) }
	}

	fun hentEllerOpprettAnsatt(navIdent: String): NavAnsattDbo {
		val navAnsatt = navAnsattRepository.get(navIdent)

		if (navAnsatt != null) return navAnsatt

		val nyNavAnsatt = nomClient.hentNavAnsatt(navIdent)

		if (nyNavAnsatt == null) {
			log.error("Klarte ikke å hente nav ansatt med ident $navIdent")
			throw IllegalArgumentException("Klarte ikke å finne nav ansatt med ident")
		}

		log.info("Oppretter ny nav ansatt for nav ident $navIdent")

		val navEnhet = nyNavAnsatt.navEnhetNummer?.let { navEnhetService.hentEllerOpprettNavEnhet(it) }

		val ansatt =
			NavAnsattDbo(
				id = UUID.randomUUID(),
				navIdent = nyNavAnsatt.navIdent,
				navn = nyNavAnsatt.navn,
				epost = nyNavAnsatt.epost,
				telefon = nyNavAnsatt.telefonnummer,
				navEnhetId = navEnhet?.id,
			)

		return upsert(ansatt)
	}

	fun hentBrukersVeileder(brukersPersonIdent: String): NavAnsattDbo? =
		veilarboppfolgingClient.hentVeilederIdent(brukersPersonIdent)?.let {
			hentEllerOpprettAnsatt(it)
		}
}
