package no.nav.amt.person.service.nav_bruker

import no.nav.amt.person.service.nav_ansatt.NavAnsatt
import no.nav.amt.person.service.nav_enhet.NavEnhet
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
)
