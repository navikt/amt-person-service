package no.nav.amt.person.service.clients.veilarbvedtaksstotte

import no.nav.common.token_client.client.MachineToMachineTokenClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tools.jackson.databind.ObjectMapper

@Configuration(proxyBeanMethods = false)
class VeilarbvedtaksstotteConfig {
	@Bean
	fun veilarbvedtaksstotteClient(
		@Value($$"${veilarbvedtaksstotte.url}") url: String,
		@Value($$"${veilarbvedtaksstotte.scope}") veilarbvedtaksstotteScope: String,
		machineToMachineTokenClient: MachineToMachineTokenClient,
		objectMapper: ObjectMapper,
	) = VeilarbvedtaksstotteClient(
		apiUrl = "$url/veilarbvedtaksstotte",
		veilarbvedtaksstotteTokenProvider = {
			machineToMachineTokenClient.createMachineToMachineToken(
				veilarbvedtaksstotteScope,
			)
		},
		objectMapper = objectMapper,
	)
}
