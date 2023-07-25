package no.nav.amt.person.service.kafka.producer.dto

import java.util.UUID

data class NavBrukerDtoV1 (
	val personId: UUID,
	val personident: String,
	val fornavn: String,
	val mellomnavn: String?,
	val etternavn: String,
	val navVeilederId: UUID?,
	val navEnhet: NavEnhetDtoV1?,
	val telefon: String?,
	val epost: String?,
	val erSkjermet: Boolean,
)
