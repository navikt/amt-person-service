package no.nav.amt.person.service.controller.dto

import no.nav.amt.person.service.nav_bruker.Adressebeskyttelse
import no.nav.amt.person.service.nav_bruker.NavBruker
import no.nav.amt.person.service.nav_bruker.Oppfolgingsperiode
import no.nav.amt.person.service.nav_enhet.NavEnhet
import no.nav.amt.person.service.person.model.Adresse
import java.util.UUID

data class NavBrukerDto (
	val personId: UUID,
	val personident: String,
	val fornavn: String,
	val mellomnavn: String?,
	val etternavn: String,
	val navVeilederId: UUID?,
	val navEnhet: NavEnhet?,
	val telefon: String?,
	val epost: String?,
	val erSkjermet: Boolean,
	val adresse: Adresse?,
	val adressebeskyttelse: Adressebeskyttelse?,
	val oppfolgingsperioder: List<Oppfolgingsperiode>
)

fun NavBruker.toDto() = NavBrukerDto(
	personId = this.person.id,
	personident = this.person.personident,
	fornavn = this.person.fornavn,
	mellomnavn = this.person.mellomnavn,
	etternavn = this.person.etternavn,
	navVeilederId = this.navVeileder?.id,
	navEnhet = this.navEnhet,
	telefon = this.telefon,
	epost = this.epost,
	erSkjermet = this.erSkjermet,
	adresse = this.adresse,
	adressebeskyttelse = this.adressebeskyttelse,
	oppfolgingsperioder = this.oppfolgingsperioder
)
