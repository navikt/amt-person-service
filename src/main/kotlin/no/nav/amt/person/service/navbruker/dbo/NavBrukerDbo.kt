package no.nav.amt.person.service.navbruker.dbo

import no.nav.amt.person.service.navansatt.NavAnsattDbo
import no.nav.amt.person.service.navbruker.Adressebeskyttelse
import no.nav.amt.person.service.navbruker.InnsatsgruppeV1
import no.nav.amt.person.service.navbruker.NavBruker
import no.nav.amt.person.service.navbruker.Oppfolgingsperiode
import no.nav.amt.person.service.navenhet.NavEnhetDbo
import no.nav.amt.person.service.person.dbo.PersonDbo
import no.nav.amt.person.service.person.model.Adresse
import java.time.LocalDateTime
import java.util.UUID

data class NavBrukerDbo(
	val id: UUID,
	val person: PersonDbo,
	val navVeileder: NavAnsattDbo?,
	val navEnhet: NavEnhetDbo?,
	val telefon: String?,
	val epost: String?,
	val erSkjermet: Boolean,
	val adresse: Adresse?,
	val sisteKrrSync: LocalDateTime?,
	val createdAt: LocalDateTime,
	val modifiedAt: LocalDateTime,
	val adressebeskyttelse: Adressebeskyttelse?,
	val oppfolgingsperioder: List<Oppfolgingsperiode>,
	val innsatsgruppe: InnsatsgruppeV1?,
) {
	fun toModel() =
		NavBruker(
			id = id,
			person = person.toModel(),
			navVeileder = navVeileder?.toModel(),
			navEnhet = navEnhet?.toModel(),
			telefon = telefon,
			epost = epost,
			erSkjermet = erSkjermet,
			adresse = adresse,
			sisteKrrSync = sisteKrrSync,
			adressebeskyttelse = adressebeskyttelse,
			oppfolgingsperioder = oppfolgingsperioder,
			innsatsgruppe = innsatsgruppe,
		)
}
