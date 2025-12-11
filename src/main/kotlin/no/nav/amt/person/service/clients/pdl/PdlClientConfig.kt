package no.nav.amt.person.service.clients.pdl

import no.nav.amt.person.service.poststed.PoststedRepository
import no.nav.common.token_client.client.MachineToMachineTokenClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tools.jackson.databind.ObjectMapper

@Configuration(proxyBeanMethods = false)
class PdlClientConfig {
	@Bean
	fun pdlClient(
		@Value($$"${pdl.url}") url: String,
		@Value($$"${pdl.scope}") scope: String,
		machineToMachineTokenClient: MachineToMachineTokenClient,
		poststedRepository: PoststedRepository,
		objectMapper: ObjectMapper,
	) = PdlClient(
		baseUrl = url,
		tokenProvider = { machineToMachineTokenClient.createMachineToMachineToken(scope) },
		poststedRepository = poststedRepository,
		objectMapper = objectMapper,
	)
}
