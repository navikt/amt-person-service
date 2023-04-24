package no.nav.amt.person.service.nav_bruker.dbo

import java.util.*

data class NavBrukerUpsert(
	val id: UUID,
	val personId: UUID,
	val navVeilederId: UUID?,
	val navEnhetId: UUID?,
	val telefon: String?,
	val epost: String?,
	val erSkjermet: Boolean,
)
