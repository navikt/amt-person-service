package no.nav.amt.person.service.nav_ansatt

import no.nav.amt.person.service.clients.nom.NomClient
import no.nav.amt.person.service.clients.nom.NomNavAnsatt
import no.nav.amt.person.service.nav_enhet.NavEnhet
import no.nav.amt.person.service.nav_enhet.NavEnhetService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class NavAnsattUpdater(
	private val navAnsattService: NavAnsattService,
	private val nomClient: NomClient,
	private val navEnhetService: NavEnhetService,
) {

	private val log = LoggerFactory.getLogger(javaClass)

	fun oppdaterAlle(batchSize: Int = 100) {
		val ansattBatcher = navAnsattService.getAll().chunked(batchSize)

		ansattBatcher.forEach { batch ->
			val ansatte = batch.associate { it.navIdent to AnsattSomSkalOppdateres(it, false) }
			val nomResultat = nomClient.hentNavAnsatte(ansatte.keys.toList())

			val oppdaterteAnsatte = nomResultat.mapNotNull { nomAnsatt ->
				ansatte[nomAnsatt.navIdent]?.let { finnOppdatering(it, nomAnsatt) }
			}

			navAnsattService.upsertMany(oppdaterteAnsatte)

			ansatte.forEach { (navIdent, ansatt) ->
				if (!ansatt.erSjekket) {
					log.warn("Fant ikke nav ansatt med ident=${navIdent} id=${ansatt.lagretAnsatt.id} i NOM")
				}
			}
		}
	}

	private fun finnOppdatering(
		ansatt: AnsattSomSkalOppdateres,
		nomAnsatt: NomNavAnsatt,
	): NavAnsatt? {
		ansatt.erSjekket = true
		val navEnhet = nomAnsatt.navEnhetNummer?.let {  navEnhetService.hentEllerOpprettNavEnhet(it) }

		return if (ansatt.lagretAnsatt.skalOppdateres(nomAnsatt, navEnhet)) {
			val telefon = nomAnsatt.telefonnummer ?: ansatt.lagretAnsatt.telefon?.also {
				log.warn("Telefonnummer for nav-ansatt ${ansatt.lagretAnsatt.id} er nullstilt i response fra Nom, ignorerer oppdatering.")
			}

			val epost = nomAnsatt.epost ?: ansatt.lagretAnsatt.epost?.also {
				log.warn("Epost for nav-ansatt ${ansatt.lagretAnsatt.id} er nullstilt i response fra Nom, ignorerer oppdatering.")
			}

			NavAnsatt(
				id = ansatt.lagretAnsatt.id,
				navIdent = nomAnsatt.navIdent,
				navn = nomAnsatt.navn,
				epost = epost,
				telefon = telefon,
				navEnhetId = navEnhet?.id
			)
		} else {
			null
		}
	}

	data class AnsattSomSkalOppdateres(
		val lagretAnsatt: NavAnsatt,
		var erSjekket: Boolean,
	)
}

private fun NavAnsatt.skalOppdateres(
	nomNavAnsatt: NomNavAnsatt,
	navEnhet: NavEnhet?
): Boolean {
	return this.navn != nomNavAnsatt.navn ||
		this.epost != nomNavAnsatt.epost ||
		this.telefon != nomNavAnsatt.telefonnummer ||
		this.navEnhetId != navEnhet?.id
}
