package no.nav.amt.person.service.internal

import no.nav.amt.person.service.person.PersonRepository
import no.nav.amt.person.service.person.PersonService
import no.nav.amt.person.service.person.dbo.PersonDbo
import no.nav.amt.person.service.person.model.finnGjeldendeIdent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class PersonUpdater(
	private val personRepository: PersonRepository,
	private val personService: PersonService,
) {

	private val log = LoggerFactory.getLogger(javaClass)

	fun oppdaterPersonidenter() {
		var offset = 0
		var personer: List<PersonDbo>

		do {
			personer = personRepository.getAll(offset)

			personer.forEach {
				val identer = personService.hentIdenter(it.personIdent)

				personService.oppdaterPersonIdent(identer)

				finnGjeldendeIdent(identer).onSuccess { ident ->
					if (ident.ident != it.personIdent) {
						log.info("Ny gjeldende ident for person ${it.id}")
					}
				}
			}
			log.info("Oppdaterte personidenter for personer fra offset $offset til ${offset + personer.size}")
			offset += personer.size
		} while (personer.isNotEmpty())
	}
}
