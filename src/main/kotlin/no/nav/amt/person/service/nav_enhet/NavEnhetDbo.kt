package no.nav.amt.person.service.nav_enhet

import java.time.LocalDateTime
import java.util.*

data class NavEnhetDbo(
	val id: UUID,
	val enhetId: String,
	val navn: String,
	val createdAt: LocalDateTime,
	val modifiedAt: LocalDateTime,
) {
	fun toModel() = NavEnhet(
		id = this.id,
		enhetId = this.enhetId,
		navn = this.navn
	)
}
