package no.nav.amt.person.service.data

import no.nav.amt.person.service.nav_enhet.NavEnhetDbo
import no.nav.amt.person.service.person.model.IdentType
import no.nav.amt.person.service.person.dbo.PersonDbo
import java.time.LocalDateTime
import java.util.*

object TestData {


	fun randomIdent(): String {
		return (10_00_19_00_00_000 .. 31_12_20_99_99_999).random().toString()
	}

	fun lagPerson(
		id: UUID = UUID.randomUUID(),
		personIdent: String = randomIdent(),
		personIdentType: IdentType? = IdentType.FOLKEREGISTERIDENT,
		fornavn: String = "Fornavn",
		mellomnavn: String? = null,
		etternavn: String = "Etternavn",
		historiskeIdenter: List<String> = emptyList(),
		createdAt: LocalDateTime = LocalDateTime.now(),
		modifiedAt: LocalDateTime = LocalDateTime.now(),
	) = PersonDbo(
			id,
			personIdent,
			personIdentType,
			fornavn,
			mellomnavn,
			etternavn,
			historiskeIdenter,
			createdAt,
			modifiedAt
	)

	fun lagNavEnhet(
		id: UUID = UUID.randomUUID(),
		enhetId: String = (1000 .. 9999).random().toString(),
		navn: String = "Nav Enhet 1",
		createdAt: LocalDateTime = LocalDateTime.now(),
		modifiedAt: LocalDateTime = LocalDateTime.now(),
	) = NavEnhetDbo(id, enhetId, navn, createdAt, modifiedAt)
}
