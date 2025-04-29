package no.nav.amt.person.service.clients.nom

import java.time.LocalDate

data class NomNavAnsatt(
	val navIdent: String,
	val navn: String,
	val telefonnummer: String?,
	val epost: String?,
	private val orgTilknytning: List<NomQueries.HentRessurser.OrgTilknytning>,
) {
	val navEnhetNummer: String get() =
		orgTilknytning
			.filter { it.erDagligOppfolging && it.gyldigFom <= LocalDate.now() }
			.maxBy { it.gyldigFom }
			.orgEnhet.remedyEnhetId ?: throw IllegalStateException("Nav enhet for ansatt mangler remedyEnhetId")

}
