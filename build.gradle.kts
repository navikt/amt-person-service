import org.jetbrains.kotlin.gradle.tasks.KotlinCompile


plugins {
	val kotlinVersion = "1.8.22"

	id("org.springframework.boot") version "3.1.0"
	id("io.spring.dependency-management") version "1.1.0"
	id("com.github.davidmc24.gradle.plugin.avro") version "1.7.1"
	kotlin("plugin.serialization") version kotlinVersion
	kotlin("jvm") version kotlinVersion
	kotlin("plugin.spring") version kotlinVersion
}

group = "no.nav.amt-person-service"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
	mavenCentral()
	maven { setUrl("https://jitpack.io") }
	maven { setUrl("https://packages.confluent.io/maven/") }
}

val commonVersion = "3.2023.05.02_06.50-0576b4e09008"
val okhttp3Version = "4.11.0"
val kotestVersion = "5.6.2"
val poaoTilgangVersion = "2023.05.02_09.15-64228b754508"
val testcontainersVersion = "1.18.3"
val tokenSupportVersion = "3.1.0"
val mockkVersion = "1.13.5"
val lang3Version = "3.12.0"
val shedlockVersion = "5.4.0"
val confluentVersion = "7.3.3"
val flywayVersion = "9.19.4"
val jacksonVersion = "2.15.2"
val micrometerVersion = "1.11.1"
val postgresVersion = "42.6.0"
val mockOauth2ServerVersion = "0.5.8"

dependencies {
	implementation("org.springframework.boot:spring-boot-starter")
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-configuration-processor")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-logging")
	implementation("org.springframework.boot:spring-boot-starter-data-jdbc")

	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlin:kotlin-stdlib")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
	implementation("com.squareup.okhttp3:okhttp:$okhttp3Version")
	implementation("org.flywaydb:flyway-core:$flywayVersion")

	implementation("io.micrometer:micrometer-registry-prometheus:$micrometerVersion")
	implementation("com.github.navikt.common-java-modules:log:$commonVersion")
	implementation("com.github.navikt.common-java-modules:token-client:$commonVersion")
	implementation("com.github.navikt.common-java-modules:rest:$commonVersion")
	implementation("com.github.navikt.common-java-modules:job:$commonVersion")
	implementation("com.github.navikt.common-java-modules:kafka:$commonVersion")

	implementation("io.confluent:kafka-avro-serializer:$confluentVersion")

	implementation("com.github.navikt.poao-tilgang:client:$poaoTilgangVersion")

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
