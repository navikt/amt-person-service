package no.nav.amt.person.service.internal

import no.nav.amt.person.service.person.PersonRepository
import no.nav.amt.person.service.person.dbo.PersonDbo
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class PersonUpdater(
	private val personRepository: PersonRepository,
) {

	private val log = LoggerFactory.getLogger(javaClass)

	fun oppdaterPersonNavnCasing() {
		var offset = 0
		var personer: List<PersonDbo>

		do {
			personer = personRepository.getAll(offset)

			personer.forEach {
				personRepository.upsert(it.toModel())
			}
			log.info("Oppdaterte casing på navn for personer fra offset $offset til ${offset + personer.size}")
			offset += personer.size
		} while (personer.isNotEmpty())
	}
}
