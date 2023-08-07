package no.nav.amt.person.service.nav_ansatt

import no.nav.amt.person.service.clients.nom.NomClient
import no.nav.amt.person.service.clients.nom.NomNavAnsatt
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class NavAnsattUpdater(
	private val navAnsattService: NavAnsattService,
	private val nomClient: NomClient,
) {

	private val log = LoggerFactory.getLogger(javaClass)

	fun oppdaterAlle(batchSize: Int = 100) {
		val ansattBatcher = navAnsattService.getAll().chunked(batchSize)

		ansattBatcher.forEach { batch ->
			val ansatte = batch.associate { it.navIdent to AnsattSomSkalOppdateres(it, false) }
			val nomResultat = nomClient.hentNavAnsatte(ansatte.keys.toList())

			val oppdaterteAnsatte = nomResultat.mapNotNull { nomAnsatt ->

				val ansatt = ansatte[nomAnsatt.navIdent] ?: return@mapNotNull null
				ansatt.erSjekket = true

				return@mapNotNull if (ansatt.lagretAnsatt.skalOppdateres(nomAnsatt)) {
					NavAnsatt(
						id = ansatt.lagretAnsatt.id,
						navIdent = nomAnsatt.navIdent,
						navn = nomAnsatt.navn,
						epost = nomAnsatt.epost,
						telefon = nomAnsatt.telefonnummer,
					)
				} else {
					null
				}
			}

			navAnsattService.upsertMany(oppdaterteAnsatte)

			ansatte.forEach { (navIdent, ansatt) ->
				if (!ansatt.erSjekket) {
					log.warn("Fant ikke nav ansatt med ident=${navIdent} id=${ansatt.lagretAnsatt.id} i NOM")
				}
			}
		}
	}

	data class AnsattSomSkalOppdateres(
		val lagretAnsatt: NavAnsatt,
		var erSjekket: Boolean,
	)
}

private fun NavAnsatt.skalOppdateres(nomNavAnsatt: NomNavAnsatt): Boolean {
	return this.navn != nomNavAnsatt.navn ||
		this.epost != nomNavAnsatt.epost ||
		this.telefon != nomNavAnsatt.telefonnummer
}
