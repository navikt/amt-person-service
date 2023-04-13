package no.nav.amt.person.service.clients.nom

import no.nav.common.token_client.client.MachineToMachineTokenClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class NomConfig {

	@Value("\${nom.url}")
	lateinit var url: String

	@Value("\${nom.scope}")
	lateinit var scope: String

	@Bean
	fun nomClient(machineToMachineTokenClient: MachineToMachineTokenClient) : NomClient {

		return NomClient(
			url = url,
			tokenSupplier = { machineToMachineTokenClient.createMachineToMachineToken(scope) }
		)
	}
}
