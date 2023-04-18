package no.nav.amt.person.service.nav_enhet

import no.nav.amt.person.service.clients.norg.NorgClient
import no.nav.amt.person.service.clients.veilarbarena.VeilarbarenaClient
import no.nav.amt.person.service.config.SecureLog.secureLog
import org.springframework.stereotype.Service
import java.util.*

@Service
class NavEnhetService(
	private val navEnhetRepository: NavEnhetRepository,
	private val norgClient: NorgClient,
	private val veilarbarenaClient: VeilarbarenaClient
) {

	fun hentNavEnhetForBruker(personIdent: String): NavEnhet? {
		val oppfolgingsenhetId = veilarbarenaClient.hentBrukerOppfolgingsenhetId(personIdent) ?: return null

		return hentEllerOpprettNavEnhet(oppfolgingsenhetId)
			.also {
				if (it == null) {
					secureLog.warn("Bruker med personIdent=$personIdent har enhetId=$oppfolgingsenhetId som ikke finnes i norg")
				}
			}
	}

	fun hentEllerOpprettNavEnhet(enhetId: String) = navEnhetRepository.get(enhetId)?.toModel() ?: opprettEnhet(enhetId)

	fun hentNavEnhet(id: UUID) = navEnhetRepository.get(id).toModel()

	private fun opprettEnhet(enhetId: String): NavEnhet? {
		val norgEnhet = norgClient.hentNavEnhet(enhetId) ?: return null

		val enhet = NavEnhet(
			id = UUID.randomUUID(),
			enhetId = enhetId,
			navn = norgEnhet.navn
		)

		navEnhetRepository.insert(enhet)

		return enhet
	}

}
