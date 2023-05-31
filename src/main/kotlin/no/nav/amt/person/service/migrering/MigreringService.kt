package no.nav.amt.person.service.migrering

import no.nav.amt.person.service.nav_bruker.NavBrukerService
import no.nav.amt.person.service.utils.JsonUtils
import org.springframework.stereotype.Service

@Service
class MigreringService(
	private val migreringRepository: MigreringRepository,
	private val navBrukerService: NavBrukerService,
) {

	fun migrerNavBruker(migreringNavBruker: MigreringNavBruker) {
		try {
			val bruker = navBrukerService.hentEllerOpprettNavBruker(migreringNavBruker.personIdent, migreringNavBruker.id)
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

}
