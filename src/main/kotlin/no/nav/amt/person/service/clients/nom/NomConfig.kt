package no.nav.amt.person.service.clients.nom

import no.nav.common.token_client.client.MachineToMachineTokenClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tools.jackson.databind.ObjectMapper

@Configuration(proxyBeanMethods = false)
class NomConfig {
	@Bean
	fun nomClient(
		@Value($$"${nom.url}") url: String,
		@Value($$"${nom.scope}") scope: String,
		@Value($$"${nom.mock:false}") mock: Boolean,
		machineToMachineTokenClient: MachineToMachineTokenClient,
		objectMapper: ObjectMapper,
	): NomClient =
		if (mock) {
			NomClientMock()
		} else {
			NomClientImpl(
				url = url,
				tokenSupplier = { machineToMachineTokenClient.createMachineToMachineToken(scope) },
				objectMapper = objectMapper,
			)
		}
}
