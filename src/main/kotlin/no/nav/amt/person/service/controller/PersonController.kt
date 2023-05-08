package no.nav.amt.person.service.controller

import no.nav.amt.person.service.controller.dto.*
import no.nav.amt.person.service.controller.request.ArrangorAnsattRequest
import no.nav.amt.person.service.controller.request.NavAnsattRequest
import no.nav.amt.person.service.controller.request.NavBrukerRequest
import no.nav.amt.person.service.controller.request.NavEnhetRequest
import no.nav.amt.person.service.nav_ansatt.NavAnsattService
import no.nav.amt.person.service.nav_bruker.NavBrukerService
import no.nav.amt.person.service.nav_enhet.NavEnhetService
import no.nav.amt.person.service.person.PersonService
import no.nav.amt.person.service.person.RolleService
import no.nav.amt.person.service.person.model.Rolle
import no.nav.security.token.support.core.api.Protected
import org.springframework.web.bind.annotation.*


@RestController
@RequestMapping("/api")
class PersonController (
	private val personService: PersonService,
	private val navAnsattService: NavAnsattService,
	private val navBrukerService: NavBrukerService,
	private val navEnhetService: NavEnhetService,
	private val rolleService: RolleService,
) {

	@Protected
	@PostMapping("/nav-bruker")
	fun hentEllerOpprettNavBruker(
		@RequestBody request: NavBrukerRequest
	): NavBrukerDto {
		return navBrukerService.hentEllerOpprettNavBruker(request.personIdent).toDto()
	}

	@Protected
	@PostMapping("/nav-ansatt")
	fun hentEllerOpprettNavAnsatt(
		@RequestBody request: NavAnsattRequest
	): NavAnsattDto {
		return navAnsattService.hentEllerOpprettAnsatt(request.navIdent).toDto()
	}

	@Protected
	@PostMapping("/arrangor-ansatt")
	fun hentEllerOpprettArrangorAnsatt(
		@RequestBody request: ArrangorAnsattRequest,
	): ArrangorAnsattDto {
		val person = personService.hentEllerOpprettPerson(request.personIdent)
		rolleService.opprettRolle(person.id, Rolle.ARRANGOR_ANSATT)
		return person.toArrangorAnsattDto()
	}
	@Protected
	@PostMapping("/nav-enhet")
	fun hentEllerOpprettNavEnhet(
		@RequestBody request: NavEnhetRequest
	): NavEnhetDto {
		return navEnhetService.hentEllerOpprettNavEnhet(request.enhetId)?.toDto()
			?: throw NoSuchElementException("Klarte ikke å hente Nav enhet med enhet id: ${request.enhetId}")
	}
}
