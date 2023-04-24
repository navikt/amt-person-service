package no.nav.amt.person.service.nav_bruker.dbo

import no.nav.amt.person.service.nav_ansatt.NavAnsattDbo
import no.nav.amt.person.service.nav_bruker.NavBruker
import no.nav.amt.person.service.nav_enhet.NavEnhetDbo
import no.nav.amt.person.service.person.dbo.PersonDbo
import java.time.LocalDateTime
import java.util.*

data class NavBrukerDbo(
	val id: UUID,
	val person: PersonDbo,
	val navVeileder: NavAnsattDbo?,
	val navEnhet: NavEnhetDbo?,
	val telefon: String?,
	val epost: String?,
	val erSkjermet: Boolean,
	val createdAt: LocalDateTime,
	val modifiedAt: LocalDateTime
) {
	fun toModel() = NavBruker(
		id,
		person.toModel(),
		navVeileder?.toModel(),
		navEnhet?.toModel(),
		telefon,
		epost,
		erSkjermet,
	)
}
