package no.nav.amt.person.service.navbruker.dbo

import no.nav.amt.person.service.navansatt.NavAnsattDbo
import no.nav.amt.person.service.navbruker.Adressebeskyttelse
import no.nav.amt.person.service.navbruker.InnsatsgruppeV1
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
	val adressebeskyttelse: Adressebeskyttelse?,
	val oppfolgingsperioder: List<Oppfolgingsperiode>,
	val innsatsgruppe: InnsatsgruppeV1?,
	val createdAt: LocalDateTime = LocalDateTime.now(),
	val modifiedAt: LocalDateTime = LocalDateTime.now(),
) {
	fun toUpsert() =
		NavBrukerUpsert(
			id = this.id,
			personId = this.person.id,
			navVeilederId = this.navVeileder?.id,
			navEnhetId = this.navEnhet?.id,
			telefon = this.telefon,
			epost = this.epost,
			erSkjermet = this.erSkjermet,
			adresse = this.adresse,
			sisteKrrSync = this.sisteKrrSync,
			adressebeskyttelse = this.adressebeskyttelse,
			oppfolgingsperioder = this.oppfolgingsperioder,
			innsatsgruppe = this.innsatsgruppe,
		)
}
