package no.nav.amt.person.service.clients.veilarboppfolging

import no.nav.common.token_client.client.MachineToMachineTokenClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tools.jackson.databind.ObjectMapper

@Configuration(proxyBeanMethods = false)
class VeilarboppfolgingConfig {
	@Bean
	fun veilarboppfolgingClient(
		@Value($$"${veilarboppfolging.url}") url: String,
		@Value($$"${veilarboppfolging.scope}") veilarboppfolgingScope: String,
		machineToMachineTokenClient: MachineToMachineTokenClient,
		objectMapper: ObjectMapper,
	) = VeilarboppfolgingClient(
		apiUrl = "$url/veilarboppfolging",
		veilarboppfolgingTokenProvider = {
			machineToMachineTokenClient.createMachineToMachineToken(
				veilarboppfolgingScope,
			)
		},
		objectMapper = objectMapper,
	)
}
