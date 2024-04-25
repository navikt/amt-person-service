package no.nav.amt.person.service.nav_bruker

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class Oppfolgingsperiode(
    val id: UUID,
    val startdato: LocalDateTime,
    val sluttdato: LocalDateTime?,
) {
	fun erAktiv(): Boolean {
		val now = LocalDate.now()
		val antallDagerGracePeriod = 28L
		return !(now.isBefore(startdato.toLocalDate()) || (sluttdato != null && now.isAfter(sluttdato.toLocalDate().plusDays(antallDagerGracePeriod))))
	}
}

fun harAktivOppfolgingsperiode(oppfolgingsperioder: List<Oppfolgingsperiode>): Boolean {
	return oppfolgingsperioder.find { it.erAktiv() } != null
}
