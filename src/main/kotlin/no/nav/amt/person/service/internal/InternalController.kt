package no.nav.amt.person.service.internal

import jakarta.servlet.http.HttpServletRequest
import no.nav.amt.person.service.nav_bruker.NavBrukerService
import no.nav.amt.person.service.person.PersonService
import no.nav.amt.person.service.utils.EnvUtils.isDev
import no.nav.common.job.JobRunner
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/internal")
class InternalController(
	private val personService: PersonService,
	private val navBrukerService: NavBrukerService,
	private val personUpdater: PersonUpdater,
) {

	@Unprotected
	@PostMapping("/person/{dollyIdent}")
	fun opprettPerson(
		servlet: HttpServletRequest,
		@PathVariable("dollyIdent") dollyIdent: String,
	) {
		if (isDev() && isInternal(servlet)) {
			personService.hentEllerOpprettPerson(dollyIdent)
		}
	}

	@Unprotected
	@PostMapping("/nav-bruker/{dollyIdent}")
	fun opprettNavBruker(
		servlet: HttpServletRequest,
		@PathVariable("dollyIdent") dollyIdent: String,
	) {
		if (isDev() && isInternal(servlet)) {
			navBrukerService.hentEllerOpprettNavBruker(dollyIdent)
		}
	}

	@Unprotected
	@PostMapping("/person/navn-casing")
	fun opprettPerson(
		servlet: HttpServletRequest,
	) {
		if (isInternal(servlet)) {
			JobRunner.run("oppdater_navn_casing", personUpdater::oppdaterPersonNavnCasing)
		}
	}

	private fun isInternal(servlet: HttpServletRequest): Boolean {
		return servlet.remoteAddr == "127.0.0.1"
	}
}
