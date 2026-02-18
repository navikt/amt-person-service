package no.nav.amt.person.service.api

import no.nav.amt.person.service.api.auth.AuthService
import no.nav.amt.person.service.api.auth.Issuer
import no.nav.amt.person.service.api.dto.AdressebeskyttelseDto
import no.nav.amt.person.service.api.dto.ArrangorAnsattDto
import no.nav.amt.person.service.api.dto.NavAnsattDto
import no.nav.amt.person.service.api.dto.NavBrukerDto
import no.nav.amt.person.service.api.dto.NavBrukerFodselsdatoDto
import no.nav.amt.person.service.api.dto.NavEnhetDto
import no.nav.amt.person.service.api.request.AdressebeskyttelseRequest
import no.nav.amt.person.service.api.request.ArrangorAnsattRequest
import no.nav.amt.person.service.api.request.NavAnsattRequest
import no.nav.amt.person.service.api.request.NavBrukerRequest
import no.nav.amt.person.service.api.request.NavEnhetRequest
import no.nav.amt.person.service.clients.krr.Kontaktinformasjon
import no.nav.amt.person.service.clients.pdl.PdlClient
import no.nav.amt.person.service.navansatt.NavAnsattRepository
import no.nav.amt.person.service.navansatt.NavAnsattService
import no.nav.amt.person.service.navbruker.NavBrukerService
import no.nav.amt.person.service.navenhet.NavEnhetRepository
import no.nav.amt.person.service.navenhet.NavEnhetService
import no.nav.amt.person.service.person.ArrangorAnsattService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api")
class PersonApiController(
	private val navAnsattRepository: NavAnsattRepository,
	private val navAnsattService: NavAnsattService,
	private val navBrukerService: NavBrukerService,
	private val navEnhetsRepository: NavEnhetRepository,
	private val navEnhetService: NavEnhetService,
	private val arrangorAnsattService: ArrangorAnsattService,
	private val pdlClient: PdlClient,
	private val authService: AuthService,
) {
	@ProtectedWithClaims(issuer = Issuer.AZURE_AD)
	@PostMapping("/nav-bruker")
	fun hentEllerOpprettNavBruker(
		@RequestBody request: NavBrukerRequest,
	): NavBrukerDto {
		authService.verifyRequestIsMachineToMachine()
		return NavBrukerDto.fromDbo(
			navBrukerService.hentEllerOpprettNavBruker(request.personident),
		)
	}

	@ProtectedWithClaims(issuer = Issuer.AZURE_AD)
	@PostMapping("/nav-bruker-fodselsar")
	fun hentNavBrukerFodselsar(
		@RequestBody request: NavBrukerRequest,
	): NavBrukerFodselsdatoDto {
		authService.verifyRequestIsMachineToMachine()
		return NavBrukerFodselsdatoDto(pdlClient.hentPersonFodselsar(request.personident))
	}

	@ProtectedWithClaims(issuer = Issuer.AZURE_AD)
	@PostMapping("/nav-bruker/kontaktinformasjon")
	fun hentNavBrukerKontaktinformasjon(
		@RequestBody personidenter: Set<String>,
	): Map<String, Kontaktinformasjon> {
		authService.verifyRequestIsMachineToMachine()
		return navBrukerService.fetchOppdatertKontaktinfo(personidenter)
	}

	@ProtectedWithClaims(issuer = Issuer.AZURE_AD)
	@PostMapping("/nav-ansatt")
	fun hentEllerOpprettNavAnsatt(
		@RequestBody request: NavAnsattRequest,
	): NavAnsattDto {
		authService.verifyRequestIsMachineToMachine()
		return NavAnsattDto.fromDbo(navAnsattService.hentEllerOpprettAnsatt(request.navIdent))
	}

	@ProtectedWithClaims(issuer = Issuer.AZURE_AD)
	@GetMapping("/nav-ansatt/{id}")
	fun hentNavAnsatt(
		@PathVariable id: UUID,
	): NavAnsattDto {
		authService.verifyRequestIsMachineToMachine()
		return NavAnsattDto.fromDbo(navAnsattRepository.get(id))
	}

	@ProtectedWithClaims(issuer = Issuer.AZURE_AD)
	@PostMapping("/arrangor-ansatt")
	fun hentEllerOpprettArrangorAnsatt(
		@RequestBody request: ArrangorAnsattRequest,
	): ArrangorAnsattDto {
		authService.verifyRequestIsMachineToMachine()
		val person = arrangorAnsattService.hentEllerOpprettAnsatt(request.personident)

		return ArrangorAnsattDto.fromDbo(person)
	}

	@ProtectedWithClaims(issuer = Issuer.AZURE_AD)
	@PostMapping("/nav-enhet")
	fun hentEllerOpprettNavEnhet(
		@RequestBody request: NavEnhetRequest,
	): NavEnhetDto {
		authService.verifyRequestIsMachineToMachine()
		return navEnhetService
			.hentEllerOpprettNavEnhet(request.enhetId)
			?.let { NavEnhetDto.fromDbo(it) }
			?: throw NoSuchElementException("Klarte ikke å hente Nav-enhet med enhet-id: ${request.enhetId}")
	}

	@ProtectedWithClaims(issuer = Issuer.AZURE_AD)
	@GetMapping("/nav-enhet/{id}")
	fun hentNavEnhet(
		@PathVariable id: UUID,
	): NavEnhetDto {
		authService.verifyRequestIsMachineToMachine()
		return NavEnhetDto.fromDbo(navEnhetsRepository.get(id))
	}

	@ProtectedWithClaims(issuer = Issuer.AZURE_AD)
	@PostMapping("/person/adressebeskyttelse")
	fun hentAdressebeskyttelse(
		@RequestBody request: AdressebeskyttelseRequest,
	): AdressebeskyttelseDto {
		authService.verifyRequestIsMachineToMachine()
		return AdressebeskyttelseDto.fromGradering(pdlClient.hentAdressebeskyttelse(request.personident))
	}
}
