package no.nav.amt.person.service.data

import no.nav.amt.person.service.clients.pdl.PdlPerson
import no.nav.amt.person.service.nav_ansatt.NavAnsattDbo
import no.nav.amt.person.service.nav_bruker.dbo.NavBrukerDbo
import no.nav.amt.person.service.nav_enhet.NavEnhetDbo
import no.nav.amt.person.service.person.dbo.PersonDbo
import no.nav.amt.person.service.person.dbo.PersonidentDbo
import no.nav.amt.person.service.person.model.AdressebeskyttelseGradering
import no.nav.amt.person.service.person.model.IdentType
import no.nav.amt.person.service.person.model.Personident
import java.time.LocalDateTime
import java.util.UUID

object TestData {

	fun randomIdent(): String {
		return (10_00_19_00_00_000 .. 31_12_20_99_99_999).random().toString()
	}

	fun randomNavIdent(): String {
		return ('A'..'Z').random().toString() + (100_000 .. 999_999).random().toString()
	}

	fun randomEnhetId() = (1000..9999).random().toString()

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
		enhetId: String = randomEnhetId(),
		navn: String = "Nav Enhet 1",
		createdAt: LocalDateTime = LocalDateTime.now(),
		modifiedAt: LocalDateTime = LocalDateTime.now(),
	) = NavEnhetDbo(id, enhetId, navn, createdAt, modifiedAt)


	fun lagNavAnsatt(
		id: UUID = UUID.randomUUID(),
		navIdent: String = randomNavIdent(),
		navn: String = "Nav Ansatt",
		telefon: String = "99988777",
		epost: String = "ansatt@nav.no",
		createdAt: LocalDateTime = LocalDateTime.now(),
		modifiedAt: LocalDateTime = LocalDateTime.now(),
	) = NavAnsattDbo(
		id = id,
		navIdent = navIdent,
		navn = navn,
		telefon = telefon,
		epost = epost,
		createdAt = createdAt,
		modifiedAt = modifiedAt,
	)

	fun lagNavBruker(
		id: UUID = UUID.randomUUID(),
		person: PersonDbo = lagPerson(),
		navVeileder: NavAnsattDbo? = lagNavAnsatt(),
		navEnhet: NavEnhetDbo? = lagNavEnhet(),
		telefon: String? = "77788999",
		epost: String? = "nav_bruker@gmail.com",
		erSkjermet: Boolean = false,
		createdAt: LocalDateTime = LocalDateTime.now(),
		modifiedAt: LocalDateTime = LocalDateTime.now(),
	) = NavBrukerDbo(id, person, navVeileder, navEnhet, telefon, epost, erSkjermet, createdAt, modifiedAt)

	fun lagPdlPerson(
		person: PersonDbo,
		telefon: String? = null,
		adressebeskyttelseGradering: AdressebeskyttelseGradering? = null,
		identer: List<Personident> = listOf(Personident(person.personIdent, false, IdentType.FOLKEREGISTERIDENT)),
	) = PdlPerson(
		fornavn = person.fornavn,
		mellomnavn = person.mellomnavn,
		etternavn = person.etternavn,
		telefonnummer = telefon,
		adressebeskyttelseGradering = adressebeskyttelseGradering,
		identer = identer,
	)

	fun lagPersonident(
		ident: String = randomIdent(),
		personId: UUID = UUID.randomUUID(),
		historisk: Boolean = false,
		type: IdentType = IdentType.FOLKEREGISTERIDENT,
		modifiedAt: LocalDateTime = LocalDateTime.now(),
		createdAt: LocalDateTime = LocalDateTime.now()
	) = PersonidentDbo(ident, personId, historisk, type, modifiedAt, createdAt)
}
