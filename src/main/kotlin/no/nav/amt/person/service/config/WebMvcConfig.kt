package no.nav.amt.person.service.config

import no.nav.amt.person.service.api.auth.MachineToMachineInterceptor
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration(proxyBeanMethods = false)
class WebMvcConfig(
    private val machineToMachineInterceptor: MachineToMachineInterceptor,
) : WebMvcConfigurer {
    override fun addInterceptors(registry: InterceptorRegistry) {
        registry
            .addInterceptor(machineToMachineInterceptor)
            .addPathPatterns("/api/**")
    }
}
