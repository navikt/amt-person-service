package no.nav.amt.person.service.clients.kodeverk

import no.nav.common.rest.client.RestClient
import okhttp3.OkHttpClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class KodeverkClientConfig {

	@Bean
	fun kodeverkHttpClient(): OkHttpClient {
		return RestClient.baseClient()
	}
}
