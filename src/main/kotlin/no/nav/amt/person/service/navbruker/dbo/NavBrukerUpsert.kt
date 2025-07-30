package no.nav.amt.person.service.navbruker.dbo

import no.nav.amt.person.service.navbruker.Adressebeskyttelse
import no.nav.amt.person.service.navbruker.InnsatsgruppeV1
import no.nav.amt.person.service.navbruker.Oppfolgingsperiode
import no.nav.amt.person.service.person.model.Adresse
import java.time.LocalDateTime
import java.util.UUID

data class NavBrukerUpsert(
	val id: UUID,
	val personId: UUID,
	val navVeilederId: UUID?,
	val navEnhetId: UUID?,
	val telefon: String?,
	val epost: String?,
	val erSkjermet: Boolean,
	val adresse: Adresse?,
	val sisteKrrSync: LocalDateTime? = null,
	val adressebeskyttelse: Adressebeskyttelse?,
	val oppfolgingsperioder: List<Oppfolgingsperiode>,
	val innsatsgruppe: InnsatsgruppeV1?,
)
