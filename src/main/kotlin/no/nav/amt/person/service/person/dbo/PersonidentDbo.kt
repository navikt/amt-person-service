package no.nav.amt.person.service.person.dbo

import no.nav.amt.person.service.person.model.IdentType
import no.nav.amt.person.service.person.model.Personident
import java.time.LocalDateTime
import java.util.UUID

data class PersonidentDbo(
	val ident: String,
	val personId: UUID,
	val historisk: Boolean,
	val type: IdentType,
	val modifiedAt: LocalDateTime,
	val createdAt: LocalDateTime,
) {
	fun toModel() = Personident(
		this.ident,
		this.historisk,
		this.type
	)
}
