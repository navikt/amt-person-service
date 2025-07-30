package no.nav.amt.person.service.navbruker

import no.nav.amt.person.service.navansatt.NavAnsatt
import no.nav.amt.person.service.navbruker.dbo.NavBrukerUpsert
import no.nav.amt.person.service.navenhet.NavEnhet
import no.nav.amt.person.service.person.model.Adresse
import no.nav.amt.person.service.person.model.Person
import java.time.LocalDateTime
import java.util.UUID

data class NavBruker(
	val id: UUID,
	val person: Person,
	val navVeileder: NavAnsatt?,
	val navEnhet: NavEnhet?,
	val telefon: String?,
	val epost: String?,
	val erSkjermet: Boolean,
	val adresse: Adresse?,
	val sisteKrrSync: LocalDateTime?,
	val adressebeskyttelse: Adressebeskyttelse?,
	val oppfolgingsperioder: List<Oppfolgingsperiode>,
	val innsatsgruppe: InnsatsgruppeV1?,
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

enum class Adressebeskyttelse {
	STRENGT_FORTROLIG,
	FORTROLIG,
	STRENGT_FORTROLIG_UTLAND,
}
