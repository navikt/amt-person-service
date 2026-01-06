package no.nav.amt.person.service.clients.veilarbarena

import no.nav.common.token_client.client.MachineToMachineTokenClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tools.jackson.databind.ObjectMapper

@Configuration(proxyBeanMethods = false)
class VeilarbarenaConfig {
	@Bean
	fun veilarbarenaClient(
		@Value($$"${veilarbarena.url}") url: String,
		@Value($$"${veilarbarena.scope}") scope: String,
		machineToMachineTokenClient: MachineToMachineTokenClient,
		objectMapper: ObjectMapper,
	) = VeilarbarenaClient(
		baseUrl = url,
		tokenProvider = { machineToMachineTokenClient.createMachineToMachineToken(scope) },
		objectMapper = objectMapper,
	)
}
