import org.jetbrains.kotlin.gradle.tasks.KotlinCompile


plugins {
	val kotlinVersion = "1.9.20"

	id("org.springframework.boot") version "3.1.5"
	id("io.spring.dependency-management") version "1.1.3"
	id("com.github.davidmc24.gradle.plugin.avro") version "1.9.1"
	kotlin("plugin.serialization") version kotlinVersion
	kotlin("jvm") version kotlinVersion
	kotlin("plugin.spring") version kotlinVersion
}

group = "no.nav.amt-person-service"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
	mavenCentral()
	maven { setUrl("https://github-package-registry-mirror.gc.nav.no/cached/maven-release") }
	maven { setUrl("https://packages.confluent.io/maven/") }
}

val commonVersion = "3.2023.10.18_13.28-58db82ecb1a5"
val okhttp3Version = "4.12.0"
val kotestVersion = "5.7.2"
val poaoTilgangVersion = "2023.09.25_09.26-72043f243cad"
val testcontainersVersion = "1.19.1"
val tokenSupportVersion = "3.1.7"
val mockkVersion = "1.13.8"
val lang3Version = "3.13.0"
val shedlockVersion = "5.9.1"
val confluentVersion = "7.3.3"
val avroVersion = "1.11.3"
val flywayVersion = "9.22.3"
val jacksonVersion = "2.15.3"
val micrometerVersion = "1.11.5"
val postgresVersion = "42.6.0"
val mockOauth2ServerVersion = "2.0.0"

dependencies {
	implementation("org.springframework.boot:spring-boot-starter")
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-configuration-processor")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-logging")
	implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
	implementation("org.yaml:snakeyaml:2.2")//overstyrer sårbar dependency

	implementation("org.springframework.retry:spring-retry")
	implementation("org.springframework:spring-aspects")

	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlin:kotlin-stdlib")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
	implementation("com.squareup.okhttp3:okhttp:$okhttp3Version")
	implementation("org.flywaydb:flyway-core:$flywayVersion")
	implementation("org.postgresql:postgresql")

	implementation("io.micrometer:micrometer-registry-prometheus:$micrometerVersion")
	implementation("no.nav.common:log:$commonVersion")
	implementation("no.nav.common:token-client:$commonVersion")
	implementation("no.nav.common:rest:$commonVersion")
	implementation("no.nav.common:job:$commonVersion")
	implementation("no.nav.common:kafka:$commonVersion")
	implementation("org.xerial.snappy:snappy-java:1.1.10.5") // overstyrer sårbar dependency

	implementation("io.confluent:kafka-avro-serializer:$confluentVersion")
	implementation("org.apache.avro:avro:$avroVersion")

	implementation("no.nav.poao-tilgang:client:$poaoTilgangVersion")

	implementation("org.apache.commons:commons-lang3:$lang3Version")

	implementation("no.nav.security:token-validation-spring:$tokenSupportVersion")

	implementation("net.javacrumbs.shedlock:shedlock-spring:$shedlockVersion")
	implementation("net.javacrumbs.shedlock:shedlock-provider-jdbc-template:$shedlockVersion")

	runtimeOnly("org.postgresql:postgresql:$postgresVersion")

	annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("io.kotest:kotest-assertions-core-jvm:$kotestVersion")
	testImplementation("io.kotest:kotest-assertions-json-jvm:$kotestVersion")
	testImplementation("com.squareup.okhttp3:mockwebserver:$okhttp3Version")
	testImplementation("org.testcontainers:testcontainers:$testcontainersVersion")
	testImplementation("org.testcontainers:postgresql:$testcontainersVersion")
	testImplementation("org.testcontainers:kafka:$testcontainersVersion")
	testImplementation("io.mockk:mockk:$mockkVersion")
	testImplementation("no.nav.security:mock-oauth2-server:$mockOauth2ServerVersion")}

tasks.getByName<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
	this.archiveFileName.set("${archiveBaseName.get()}.${archiveExtension.get()}")
}

tasks.withType<KotlinCompile> {
	kotlinOptions {
		freeCompilerArgs = listOf("-Xjsr305=strict")
		jvmTarget = "17"
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}
