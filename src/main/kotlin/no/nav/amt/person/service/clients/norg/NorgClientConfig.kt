package no.nav.amt.person.service.clients.norg

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tools.jackson.databind.ObjectMapper

@Configuration(proxyBeanMethods = false)
class NorgClientConfig {
	@Bean
	fun norgClient(
		@Value($$"${norg.url}") url: String,
		objectMapper: ObjectMapper,
	) = NorgClient(
		url = url,
		objectMapper = objectMapper,
	)
}
