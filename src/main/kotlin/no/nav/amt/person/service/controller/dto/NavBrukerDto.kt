package no.nav.amt.person.service.controller.dto

import no.nav.amt.person.service.nav_bruker.NavBruker
import no.nav.amt.person.service.nav_enhet.NavEnhet
import no.nav.amt.person.service.person.model.IdentType
import java.util.*

data class NavBrukerDto (
	val id: UUID,
	val personIdent: String,
	val personIdentType: IdentType?,
	val fornavn: String,
	val mellomnavn: String?,
	val etternavn: String,
	val navVeilederId: UUID?,
	val navEnhet: NavEnhet?,
	val telefon: String?,
	val epost: String?,
	val erSkjermet: Boolean,
)

fun NavBruker.toDto() = NavBrukerDto(
	id = this.id,
	personIdent = this.person.personIdent,
	personIdentType = this.person.personIdentType,
	fornavn = this.person.fornavn,
	mellomnavn = this.person.mellomnavn,
	etternavn = this.person.etternavn,
	navVeilederId = this.navVeileder?.id,
	navEnhet = this.navEnhet,
	telefon = this.telefon,
	epost = this.epost,
	erSkjermet = this.erSkjermet,
)
