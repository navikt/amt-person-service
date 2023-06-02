package no.nav.amt.person.service.migrering

import jakarta.servlet.http.HttpServletRequest
import no.nav.amt.person.service.controller.auth.Issuer
import no.nav.amt.person.service.nav_ansatt.NavAnsatt
import no.nav.amt.person.service.nav_ansatt.NavAnsattService
import no.nav.amt.person.service.nav_bruker.NavBruker
import no.nav.amt.person.service.nav_enhet.NavEnhet
import no.nav.amt.person.service.nav_enhet.NavEnhetService
import no.nav.amt.person.service.utils.JsonUtils
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.util.*

@RestController
@RequestMapping("/api/migrer")
class MigreringController(
	private val navEnhetService: NavEnhetService,
	private val navAnsattService: NavAnsattService,
	private val migreringRepository: MigreringRepository,
	private val migreringService: MigreringService,
) {

	@ProtectedWithClaims(issuer = Issuer.AZURE_AD)
	@PostMapping("/nav-enhet")
	fun migrerNavEnhet(
		@RequestBody request: MigreringNavEnhet,
	) {
		val enhet = navEnhetService.hentEllerOpprettNavEnhetMedId(request.enhetId, request.id)

		if (enhet == null) {
			migreringRepository.upsert(
				MigreringDbo(
					resursId = request.id,
					endepunkt = "nav-enhet",
					requestBody = JsonUtils.toJsonString(request),
					diff = null,
					error = "Fant ikke NavEnhet"
				)
			)
			return
		}

		val diffMap = request.diff(enhet)

		if (diffMap.isNotEmpty()) {
			migreringRepository.upsert(
				MigreringDbo(
					resursId = request.id,
					endepunkt = "nav-enhet",
					requestBody = JsonUtils.toJsonString(request),
					diff = JsonUtils.toJsonString(diffMap),
					error = null,
				)
			)
		}
	}

	@ProtectedWithClaims(issuer = Issuer.AZURE_AD)
	@PostMapping("/nav-ansatt")
	fun migrerNavAnsatt(
		@RequestBody request: MigreringNavAnsatt,
	) {
		migrerAnsatt(request)
	}

	@ProtectedWithClaims(issuer = Issuer.AZURE_AD)
	@PostMapping("/nav-bruker")
	fun migrerNavBruker(
		@RequestBody request: MigreringNavBruker,
	) {
		migreringService.migrerNavBruker(request)
	}

	@Unprotected
	@GetMapping("/nav-ansatt/retry/{id}")
	fun retryMigrerNavAnsatt(
		request: HttpServletRequest,
		@PathVariable("id") id: UUID,
	) {
		if (!isInternal(request)) {
			throw ResponseStatusException(HttpStatus.UNAUTHORIZED)
		}

		val dbo = migreringRepository.get(id) ?: throw NoSuchElementException()
		val body = JsonUtils.fromJsonString<MigreringNavAnsatt>(dbo.requestBody)
		val diff = migrerAnsatt(body)

		if (diff.isEmpty()) {
			migreringRepository.delete(body.id)
		}
	}

	@Unprotected
	@GetMapping("/nav-ansatt/retry")
	fun retryMigrerNavAnsatte(request: HttpServletRequest) {
		if (!isInternal(request)) {
			throw ResponseStatusException(HttpStatus.UNAUTHORIZED)
		}

		var offset = 0
		var migreringDboer: List<MigreringDbo>

		do {
			migreringDboer = migreringRepository.getAll("nav-ansatt", offset)

			migreringDboer.forEach { dbo ->
				val body = JsonUtils.fromJsonString<MigreringNavAnsatt>(dbo.requestBody)
				val diff = migrerAnsatt(body)

				if (diff.isEmpty()) {
					migreringRepository.delete(body.id)
				}
			}
			offset += migreringDboer.size
		} while (migreringDboer.isNotEmpty())
	}

	private fun migrerAnsatt(request: MigreringNavAnsatt): Map<String, DiffProperty> {
		try {
			val ansatt = navAnsattService.hentEllerOpprettAnsatt(request.navIdent, request.id)

			val diffMap = request.diff(ansatt)

			if (diffMap.isNotEmpty()) {
				migreringRepository.upsert(
					MigreringDbo(
						resursId = request.id,
						endepunkt = "nav-ansatt",
						requestBody = JsonUtils.toJsonString(request),
						diff = JsonUtils.toJsonString(diffMap),
						error = null,
					)
				)
			}

			return diffMap

		} catch (e: Exception) {
			migreringRepository.upsert(
				MigreringDbo(
					resursId = request.id,
					endepunkt = "nav-ansatt",
					requestBody = JsonUtils.toJsonString(request),
					diff = null,
					error = e.message
				)
			)
			throw (e)
		}
	}

	private fun isInternal(servlet: HttpServletRequest): Boolean {
		return servlet.remoteAddr == "127.0.0.1"
	}
}

data class MigreringNavEnhet(
	val id: UUID,
	val enhetId: String,
	val navn: String,
) {
	fun diff(navEnhet: NavEnhet): Map<String, DiffProperty> {
		val diffMap = mutableMapOf<String, DiffProperty>()
		if (this.id != navEnhet.id) diffMap["id"] = DiffProperty(this.id.toString(), navEnhet.id.toString())
		if (this.enhetId != navEnhet.enhetId) diffMap["enhetId"] = DiffProperty(this.enhetId, navEnhet.enhetId)
		if (this.navn != navEnhet.navn) diffMap["navn"] = DiffProperty(this.navn, navEnhet.navn)

		return diffMap
	}
}

data class MigreringNavAnsatt(
	val id: UUID,
	val navIdent: String,
	val navn: String,
	val epost: String?,
	val telefon: String?,
) {
	fun diff(navAnsatt: NavAnsatt): Map<String, DiffProperty> {
		val diffMap = mutableMapOf<String, DiffProperty>()
		if (this.id != navAnsatt.id) diffMap["id"] = DiffProperty(this.id.toString(), navAnsatt.id.toString())
		if (this.navIdent != navAnsatt.navIdent) diffMap["navIdent"] = DiffProperty(this.navIdent, navAnsatt.navIdent)
		if (this.navn != navAnsatt.navn) diffMap["navn"] = DiffProperty(this.navn, navAnsatt.navn)
		if (this.epost != navAnsatt.epost) diffMap["epost"] = DiffProperty(this.epost, navAnsatt.epost)
		if (this.telefon != navAnsatt.telefon) diffMap["telefon"] = DiffProperty(this.telefon, navAnsatt.telefon)

		return diffMap
	}
}


data class MigreringNavBruker(
	val id: UUID,
	val personIdent: String,
	val personIdentType: String?,
	val historiskeIdenter: List<String>,
	val fornavn: String,
	val mellomnavn: String?,
	val etternavn: String,
	val navVeilederId: UUID?,
	val navEnhetId: UUID?,
	val telefon: String?,
	val epost: String?,
	val erSkjermet: Boolean,
) {
	fun diff(navBruker: NavBruker): Map<String, DiffProperty> {

		val diffMap = mutableMapOf<String, DiffProperty>()

		if (this.id != navBruker.person.id)
			diffMap["personId"] = DiffProperty(this.id.toString(), navBruker.person.id.toString())
		if (this.personIdent != navBruker.person.personIdent)
			diffMap["personIdent"] = DiffProperty(this.personIdent, navBruker.person.personIdent)
		if (this.personIdentType != null && this.personIdentType.toString() != navBruker.person.personIdentType.toString())
			diffMap["personIdentType"] = DiffProperty(this.personIdentType.toString(), navBruker.person.personIdentType.toString())
		if (this.fornavn != navBruker.person.fornavn)
			diffMap["fornavn"] = DiffProperty(this.fornavn, navBruker.person.fornavn)
		if (this.mellomnavn != navBruker.person.mellomnavn)
			diffMap["mellomnavn"] = DiffProperty(this.mellomnavn, navBruker.person.mellomnavn)
		if (this.etternavn != navBruker.person.etternavn)
			diffMap["etternavn"] = DiffProperty(this.etternavn, navBruker.person.etternavn)
		if (this.telefon != navBruker.telefon)
			diffMap["telefon"] = DiffProperty(this.telefon, navBruker.telefon)
		if (this.epost != navBruker.epost)
			diffMap["epost"] = DiffProperty(this.epost, navBruker.epost)
		if (this.erSkjermet != navBruker.erSkjermet)
			diffMap["erSkjermet"] = DiffProperty(this.erSkjermet.toString(), navBruker.erSkjermet.toString())
		if (this.navEnhetId != navBruker.navEnhet?.id)
			diffMap["navEnhetId"] = DiffProperty(this.navEnhetId.toString(), navBruker.navEnhet?.id.toString())
		if (this.navVeilederId != navBruker.navVeileder?.id)
			diffMap["navVeilederId"] = DiffProperty(this.navVeilederId.toString(), navBruker.navVeileder?.id.toString())

		return diffMap
	}
}

data class DiffProperty(
	val amtTiltak: String?,
	val amtPerson: String?,
)


