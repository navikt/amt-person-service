package no.nav.amt.person.service.nav_ansatt

import no.nav.amt.person.service.clients.nom.NomClient
import no.nav.amt.person.service.clients.veilarboppfolging.VeilarboppfolgingClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*

@Service
 class NavAnsattService(
	private val navAnsattRepository: NavAnsattRepository,
	private val nomClient: NomClient,
	private val veilarboppfolgingClient: VeilarboppfolgingClient,
) {

	private val log = LoggerFactory.getLogger(javaClass)

	fun hentNavAnsatt(navAnsattId: UUID): NavAnsatt {
		return navAnsattRepository.get(navAnsattId).toModel()
	}

	fun hentEllerOpprettAnsatt(navIdent: String): NavAnsatt {
		val navAnsatt = navAnsattRepository.get(navIdent)

		if (navAnsatt != null)
			return navAnsatt.toModel()

		val nyNavAnsatt = nomClient.hentNavAnsatt(navIdent)

		if (nyNavAnsatt == null) {
			log.error("Klarte ikke å hente nav ansatt med ident $navIdent")
			throw IllegalArgumentException("Klarte ikke å finne nav ansatt med ident")
		}

		log.info("Oppretter ny nav ansatt for nav ident $navIdent")

		val nyAnsattId = UUID.randomUUID()

		val ansatt = NavAnsatt(
				id = nyAnsattId,
				navIdent = nyNavAnsatt.navIdent,
				navn = nyNavAnsatt.navn,
				epost = nyNavAnsatt.epost,
				telefon = nyNavAnsatt.telefonnummer,
			)

		navAnsattRepository.upsert(ansatt)

		return ansatt
	}


	fun hentBrukersVeileder(brukersPersonIdent: String): NavAnsatt? =
		veilarboppfolgingClient.hentVeilederIdent(brukersPersonIdent)?.let {
			hentEllerOpprettAnsatt(it)
		}

}
