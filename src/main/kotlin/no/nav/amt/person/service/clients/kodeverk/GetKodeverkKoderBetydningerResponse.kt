package no.nav.amt.person.service.clients.kodeverk

import no.nav.amt.person.service.poststed.Postnummer

data class GetKodeverkKoderBetydningerResponse(
	val betydninger: Map<String, List<Betydning>>,
) {
	data class Betydning(
		val beskrivelser: Map<String, Beskrivelse>,
	) {
		data class Beskrivelse(
			val term: String,
		)
	}

	fun toPostnummerListe(): List<Postnummer> =
		betydninger.map {
			Postnummer(
				postnummer = it.key,
				poststed =
					it.value
						.first()
						.beskrivelser["nb"]
						?.term
						?: throw RuntimeException("Kode ${it.key} mangler term"),
			)
		}
}
