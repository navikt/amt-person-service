package no.nav.amt.person.service.clients.pdl

import no.nav.amt.person.service.poststed.PoststedRepository
import no.nav.common.token_client.client.MachineToMachineTokenClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class PdlClientConfig {
	@Value("\${pdl.url}")
	lateinit var url: String

	@Value("\${pdl.scope}")
	lateinit var scope: String

	@Bean
	fun pdlClient(
		machineToMachineTokenClient: MachineToMachineTokenClient,
		poststedRepository: PoststedRepository,
	): PdlClient =
		PdlClient(
			baseUrl = url,
			tokenProvider = { machineToMachineTokenClient.createMachineToMachineToken(scope) },
			poststedRepository = poststedRepository,
		)
}
