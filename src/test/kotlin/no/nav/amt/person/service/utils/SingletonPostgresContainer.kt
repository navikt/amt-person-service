package no.nav.amt.person.service.utils

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy
import org.testcontainers.utility.DockerImageName
import javax.sql.DataSource

object SingletonPostgresContainer {

	private val log = LoggerFactory.getLogger(javaClass)

	private const val POSTGRES_DOCKER_IMAGE_NAME = "postgres:14-alpine"

	private var postgresContainer: PostgreSQLContainer<Nothing>? = null

	private var containerDataSource: DataSource? = null

	fun getDataSource(): DataSource {
		if (containerDataSource == null) {
			containerDataSource = createDataSource(getContainer())
		}

		return containerDataSource!!
	}

	fun getContainer(): PostgreSQLContainer<Nothing> {
		if (postgresContainer == null) {
			log.info("Starting new postgres database...")

			val container = createContainer()
			postgresContainer = container

			container.start()

			log.info("Applying database migrations...")
			applyMigrations(createDataSource(container))

			setupShutdownHook()
		}

		return postgresContainer as PostgreSQLContainer<Nothing>
	}

	fun cleanup() {
		val jdbcTemplate = JdbcTemplate(getDataSource())

		val tables: List<String> = jdbcTemplate.queryForList(
			"SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'",
			String::class.java
		)

		tables.forEach { table ->
			log.info("Dropping table $table...")
			jdbcTemplate.execute("TRUNCATE TABLE $table CASCADE")
		}
	}

	private fun applyMigrations(dataSource: DataSource) {
		val flyway: Flyway = Flyway.configure()
			.dataSource(dataSource)
			.connectRetries(10)
			.cleanDisabled(false)
			.load()

		flyway.clean()
		flyway.migrate()
		flyway.info()
	}

	private fun createContainer(): PostgreSQLContainer<Nothing> {
		val container = PostgreSQLContainer<Nothing>(DockerImageName.parse(POSTGRES_DOCKER_IMAGE_NAME).asCompatibleSubstituteFor("postgres"))
		container.addEnv("TZ", "Europe/Oslo")
		return container.waitingFor(HostPortWaitStrategy())
	}

	private fun createDataSource(container: PostgreSQLContainer<Nothing>): DataSource {
		val config = HikariConfig()

		config.jdbcUrl = container.jdbcUrl
		config.username = container.username
		config.password = container.password
		config.maximumPoolSize = 3
		config.minimumIdle = 1

		return HikariDataSource(config)
	}

	private fun setupShutdownHook() {
		Runtime.getRuntime().addShutdownHook(Thread {
			log.info("Shutting down postgres database...")
			postgresContainer?.stop()
		})
	}
}
