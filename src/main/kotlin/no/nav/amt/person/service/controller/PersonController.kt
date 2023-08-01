package no.nav.amt.person.service.controller

import no.nav.amt.person.service.controller.auth.AuthService
import no.nav.amt.person.service.controller.auth.Issuer
import no.nav.amt.person.service.controller.dto.*
import no.nav.amt.person.service.controller.request.AdressebeskyttelseRequest
import no.nav.amt.person.service.controller.request.ArrangorAnsattRequest
import no.nav.amt.person.service.controller.request.NavAnsattRequest
import no.nav.amt.person.service.controller.request.NavBrukerRequest
import no.nav.amt.person.service.controller.request.NavEnhetRequest
import no.nav.amt.person.service.nav_ansatt.NavAnsattService
import no.nav.amt.person.service.nav_bruker.NavBrukerService
import no.nav.amt.person.service.nav_enhet.NavEnhetService
import no.nav.amt.person.service.person.ArrangorAnsattService
import no.nav.amt.person.service.person.PersonService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.*
import java.util.UUID


@RestController
@RequestMapping("/api")
class PersonController (
	private val personService: PersonService,
	private val navAnsattService: NavAnsattService,
	private val navBrukerService: NavBrukerService,
	private val navEnhetService: NavEnhetService,
	private val arrangorAnsattService: ArrangorAnsattService,
	private val authService: AuthService,
) {

	@ProtectedWithClaims(issuer = Issuer.AZURE_AD)
	@PostMapping("/nav-bruker")
	fun hentEllerOpprettNavBruker(
		@RequestBody request: NavBrukerRequest
	): NavBrukerDto {
		authService.verifyRequestIsMachineToMachine()
		return navBrukerService.hentEllerOpprettNavBruker(request.personident).toDto()
	}

	@ProtectedWithClaims(issuer = Issuer.AZURE_AD)
	@PostMapping("/nav-ansatt")
	fun hentEllerOpprettNavAnsatt(
		@RequestBody request: NavAnsattRequest
	): NavAnsattDto {
		authService.verifyRequestIsMachineToMachine()
		return navAnsattService.hentEllerOpprettAnsatt(request.navIdent).toDto()
	}

	@ProtectedWithClaims(issuer = Issuer.AZURE_AD)
	@GetMapping("/nav-ansatt/{id}")
	fun hentNavAnsatt(
		@PathVariable id: UUID
	): NavAnsattDto {
		authService.verifyRequestIsMachineToMachine()
		return navAnsattService.hentNavAnsatt(id).toDto()
	}


	@ProtectedWithClaims(issuer = Issuer.AZURE_AD)
	@PostMapping("/arrangor-ansatt")
	fun hentEllerOpprettArrangorAnsatt(
		@RequestBody request: ArrangorAnsattRequest,
	): ArrangorAnsattDto {
		authService.verifyRequestIsMachineToMachine()
		val person = request.personident?.let {
			arrangorAnsattService.hentEllerOpprettAnsatt(it)
		} ?: arrangorAnsattService.hentEllerOpprettAnsatt(request.personIdent!!)

		return person.toArrangorAnsattDto()
	}

	@ProtectedWithClaims(issuer = Issuer.AZURE_AD)
	@PostMapping("/nav-enhet")
	fun hentEllerOpprettNavEnhet(
		@RequestBody request: NavEnhetRequest
	): NavEnhetDto {
		authService.verifyRequestIsMachineToMachine()
		return navEnhetService.hentEllerOpprettNavEnhet(request.enhetId)?.toDto()
			?: throw NoSuchElementException("Klarte ikke å hente Nav enhet med enhet id: ${request.enhetId}")
	}

	@ProtectedWithClaims(issuer = Issuer.AZURE_AD)
	@PostMapping("/person/adressebeskyttelse")
	fun hentAdressebeskyttelse(
		@RequestBody request: AdressebeskyttelseRequest
	): AdressebeskyttelseDto {
		authService.verifyRequestIsMachineToMachine()
		return personService.hentAdressebeskyttelse(request.personident).toDto()
	}

}
