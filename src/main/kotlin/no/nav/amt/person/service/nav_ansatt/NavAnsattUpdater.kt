package no.nav.amt.person.service.nav_ansatt

import no.nav.amt.person.service.clients.nom.NomClient
import org.springframework.stereotype.Component

@Component
class NavAnsattUpdater(
	private val nomClient: NomClient,
	private val navAnsattRepository: NavAnsattRepository,
) {

	private val batchSize = 100

	fun oppdaterAlle() {
		val ansattBatcher = navAnsattRepository.getAll()
			.map { it.navIdent }
			.chunked(batchSize)

		ansattBatcher.forEach { batch ->
			val oppdaterteAnsatte = nomClient.hentNavAnsatte(batch)
			navAnsattRepository.updateMany(oppdaterteAnsatte)
		}
	}

}
