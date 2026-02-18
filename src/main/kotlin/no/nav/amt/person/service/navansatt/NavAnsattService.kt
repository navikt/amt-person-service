package no.nav.amt.person.service.navansatt

import no.nav.amt.person.service.clients.nom.NomClient
import no.nav.amt.person.service.clients.veilarboppfolging.VeilarboppfolgingClient
import no.nav.amt.person.service.kafka.producer.KafkaProducerService
import no.nav.amt.person.service.navenhet.NavEnhetService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

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
		navAnsattRepository.get(navIdent)?.let { navAnsatt -> return navAnsatt }

		val nomNavAnsatt =
			nomClient.hentNavAnsatt(navIdent)
				?: throw IllegalArgumentException("Klarte ikke å finne nav ansatt med ident $navIdent").also {
					log.error("Klarte ikke å hente nav ansatt med ident $navIdent")
				}

		log.info("Oppretter ny nav ansatt for nav ident $navIdent")

		val navEnhet = nomNavAnsatt.navEnhetNummer?.let { navEnhetService.hentEllerOpprettNavEnhet(it) }

		return upsert(nomNavAnsatt.toNavAnsatt(navEnhet?.id))
	}

	fun hentBrukersVeileder(brukersPersonIdent: String): NavAnsattDbo? =
		veilarboppfolgingClient.hentVeilederIdent(brukersPersonIdent)?.let {
			hentEllerOpprettAnsatt(it)
		}
}
