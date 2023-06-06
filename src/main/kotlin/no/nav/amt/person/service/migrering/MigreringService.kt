package no.nav.amt.person.service.migrering

import no.nav.amt.person.service.nav_bruker.NavBrukerService
import no.nav.amt.person.service.person.PersonService
import no.nav.amt.person.service.utils.JsonUtils
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class MigreringService(
	private val migreringRepository: MigreringRepository,
	private val navBrukerService: NavBrukerService,
	private val personService: PersonService,
) {

	fun migrerNavBruker(migreringNavBruker: MigreringNavBruker) {
		try {
			personService.opprettPersonMedId(migreringNavBruker.personIdent, migreringNavBruker.id)
			val bruker = navBrukerService.hentEllerOpprettNavBruker(migreringNavBruker.personIdent)
			val diffMap = migreringNavBruker.diff(bruker)

			if (diffMap.isNotEmpty()) {
				migreringRepository.upsert(
					MigreringDbo(
						resursId = migreringNavBruker.id,
						endepunkt = "nav-bruker",
						requestBody = JsonUtils.toJsonString(migreringNavBruker),
						diff = JsonUtils.toJsonString(diffMap),
						error = null,
					)
				)
			}
		} catch (e: Exception) {
			migreringRepository.upsert(
				MigreringDbo(
					resursId = migreringNavBruker.id,
					endepunkt = "nav-bruker",
					requestBody = JsonUtils.toJsonString(migreringNavBruker),
					diff = null,
					error = e.message
				)
			)
			throw (e)
		}
	}

	fun hentMigrering(resursId: UUID): MigreringDbo? {
		return migreringRepository.get(resursId)
	}

}
