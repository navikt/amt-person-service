package no.nav.amt.person.service.person

import no.nav.amt.person.service.person.model.Rolle
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class RolleService(
	private val rolleRepository: RolleRepository,
) {
	fun opprettRolle(
		personId: UUID,
		rolle: Rolle,
	) {
		rolleRepository.insert(personId, rolle)
	}

	fun harRolle(
		personId: UUID,
		rolle: Rolle,
	): Boolean = rolleRepository.harRolle(personId, rolle)
}
