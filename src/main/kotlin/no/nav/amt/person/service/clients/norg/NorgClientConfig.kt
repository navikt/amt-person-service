package no.nav.amt.person.service.clients.norg

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class NorgClientConfig {
	@Value("\${norg.url}")
	lateinit var url: String

	@Bean
	fun norgClient(): NorgClient = NorgClient(url)
}
