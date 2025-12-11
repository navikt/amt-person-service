package no.nav.amt.person.service.clients.krr

import no.nav.common.token_client.client.MachineToMachineTokenClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tools.jackson.databind.ObjectMapper

@Configuration(proxyBeanMethods = false)
class KrrProxyClientConfig {
	@Bean
	fun krrProxyClient(
		@Value($$"${digdir-krr-proxy.url}") url: String,
		@Value($$"${digdir-krr-proxy.scope}") scope: String,
		machineToMachineTokenClient: MachineToMachineTokenClient,
		objectMapper: ObjectMapper,
	) = KrrProxyClient(
		baseUrl = url,
		tokenProvider = { machineToMachineTokenClient.createMachineToMachineToken(scope) },
		objectMapper = objectMapper,
	)
}
