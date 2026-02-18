package no.nav.amt.person.service.clients.norg

import no.nav.amt.person.service.navenhet.NavEnhetDbo

data class NorgNavEnhetDto(
	val navn: String,
	val enhetNr: String,
) {
	companion object {
		// benyttes i tester
		fun fromDbo(source: NavEnhetDbo) =
			NorgNavEnhetDto(
				enhetNr = source.enhetId,
				navn = source.navn,
			)
	}
}
