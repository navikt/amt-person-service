package no.nav.amt.person.service.nav_bruker

import no.nav.amt.person.service.nav_ansatt.NavAnsatt
import no.nav.amt.person.service.nav_bruker.dbo.NavBrukerUpsert
import no.nav.amt.person.service.nav_enhet.NavEnhet
import no.nav.amt.person.service.person.model.Adresse
import no.nav.amt.person.service.person.model.Person
import java.util.*

data class NavBruker(
	val id: UUID,
	val person: Person,
	val navVeileder: NavAnsatt?,
	val navEnhet: NavEnhet?,
	val telefon: String?,
	val epost: String?,
	val erSkjermet: Boolean,
	val adresse: Adresse?
) {
	fun toUpsert(
		id: UUID = this.id,
		personId: UUID = this.person.id,
		navVeilederId: UUID? = this.navVeileder?.id,
		navEnhetId: UUID? = this.navEnhet?.id,
		telefon: String? = this.telefon,
		epost: String? = this.epost,
		erSkjermet: Boolean = this.erSkjermet,
		adresse: Adresse? = this.adresse
	) = NavBrukerUpsert(
		id = id,
		personId = personId,
		navVeilederId = navVeilederId,
		navEnhetId = navEnhetId,
		telefon = telefon,
		epost = epost,
		erSkjermet = erSkjermet,
		adresse = adresse
	)

}


