package no.nav.amt.person.service.utils

object EnvUtils {
	fun isDev(): Boolean {
		val cluster = System.getenv("NAIS_CLUSTER_NAME") ?: "Ikke dev"
		return cluster == "dev-gcp"
	}

	fun isProd(): Boolean {
		val cluster = System.getenv("NAIS_CLUSTER_NAME") ?: "Ikke prod"
		return cluster == "prod-gcp"
	}
}
