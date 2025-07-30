package no.nav.amt.person.service.clients.krr

import no.nav.common.token_client.client.MachineToMachineTokenClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class KrrProxyClientConfig {
	@Value("\${digdir-krr-proxy.url}")
	lateinit var url: String

	@Value("\${digdir-krr-proxy.scope}")
	lateinit var scope: String

	@Bean
	fun krrProxyClient(machineToMachineTokenClient: MachineToMachineTokenClient) =
		KrrProxyClient(
			baseUrl = url,
			tokenProvider = { machineToMachineTokenClient.createMachineToMachineToken(scope) },
		)
}
