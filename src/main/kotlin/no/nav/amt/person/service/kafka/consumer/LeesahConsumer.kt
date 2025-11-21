package no.nav.amt.person.service.kafka.consumer

import no.nav.amt.person.service.navbruker.NavBrukerService
import no.nav.amt.person.service.person.PersonService
import no.nav.person.pdl.leesah.Personhendelse
import no.nav.person.pdl.leesah.adressebeskyttelse.Adressebeskyttelse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

enum class OpplysningsType {
	NAVN_V1,
	ADRESSEBESKYTTELSE_V1,
	KONTAKTADRESSE_V1,
	BOSTEDSADRESSE_V1,
	OPPHOLDSADRESSE_V1,
}

@Service
class LeesahConsumer(
	private val personService: PersonService,
	private val navBrukerService: NavBrukerService,
) {
	private val log = LoggerFactory.getLogger(javaClass)

	fun ingest(personhendelse: Personhendelse) {
		when (personhendelse.opplysningstype) {
			OpplysningsType.NAVN_V1.toString() -> handterNavn(personhendelse.personidenter)
			OpplysningsType.ADRESSEBESKYTTELSE_V1.toString() ->
				handterAdressebeskyttelse(personhendelse.personidenter, personhendelse.adressebeskyttelse)

			OpplysningsType.BOSTEDSADRESSE_V1.toString() -> handterAdresse(personhendelse.personidenter)
			OpplysningsType.KONTAKTADRESSE_V1.toString() -> handterAdresse(personhendelse.personidenter)
			OpplysningsType.OPPHOLDSADRESSE_V1.toString() -> handterAdresse(personhendelse.personidenter)
		}
	}

	private fun handterAdressebeskyttelse(
		personidenter: List<String>,
		adressebeskyttelse: Adressebeskyttelse?,
	) {
		if (adressebeskyttelse == null) {
			log.warn("Mottok melding med opplysningstype Adressebeskyttelse fra pdl-leesah men adressebeskyttelse manglet")
			return
		}

		val lagredePersonidenter = personService.hentPersoner(personidenter).map { it.personident }

		if (lagredePersonidenter.isEmpty()) return

		personidenter.forEach {
			navBrukerService.oppdaterAdressebeskyttelse(it)
		}
	}

	private fun handterNavn(personidenter: List<String>) {
		val personer = personService.hentPersoner(personidenter)

		if (personer.isEmpty()) return

		personer
			.filter { person -> person.id.toString() !in personerMedFalskIdentitet }
			.forEach { person ->
				personService.oppdaterNavn(person)
			}
	}

	private fun handterAdresse(personidenter: List<String>) {
		val lagredePersonidenter = personService.hentPersoner(personidenter).map { it.personident }

		if (lagredePersonidenter.isEmpty()) return

		navBrukerService.oppdaterAdresse(lagredePersonidenter)
	}

	companion object {
		val personerMedFalskIdentitet =
			setOf(
				"15c58a43-9fdd-4c05-98dc-30395760afa5",
				"5d4e6416-5c82-48ec-a80e-04304f6d300d",
				"aeba3431-5a7c-42ff-b7e4-a464746e519e",
				"de7785d4-6c41-41fe-9743-1bd034590ee4",
			)
	}
}
