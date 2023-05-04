package no.nav.amt.person.service.controller

import jakarta.servlet.http.HttpServletRequest
import no.nav.amt.person.service.person.PersonService
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/internal")
class InternalController(
	private val personService: PersonService
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

	private fun isInternal(servlet: HttpServletRequest): Boolean {
		return servlet.remoteAddr == "127.0.0.1"
	}

	private fun isDev(): Boolean {
		val cluster = System.getenv("NAIS_CLUSTER_NAME") ?: "Ikke dev"
		return cluster == "dev-gcp"
	}
}
