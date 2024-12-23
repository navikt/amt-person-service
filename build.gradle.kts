import org.jetbrains.kotlin.gradle.tasks.KotlinCompile


plugins {
	val kotlinVersion = "2.1.0"

	id("org.springframework.boot") version "3.4.1"
	id("io.spring.dependency-management") version "1.1.7"
	id("com.github.davidmc24.gradle.plugin.avro") version "1.9.1"
	kotlin("plugin.serialization") version kotlinVersion
	kotlin("jvm") version kotlinVersion
	kotlin("plugin.spring") version kotlinVersion
}

group = "no.nav.amt-person-service"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_21

repositories {
	mavenCentral()
	maven { setUrl("https://github-package-registry-mirror.gc.nav.no/cached/maven-release") }
	maven { setUrl("https://packages.confluent.io/maven/") }
}

val commonVersion = "3.2024.10.25_13.44-9db48a0dbe67"
val okhttp3Version = "4.12.0"
val kotestVersion = "5.9.1"
val poaoTilgangVersion = "2024.10.29_14.10-4fff597d6e1b"
val testcontainersVersion = "1.20.4"
val tokenSupportVersion = "5.0.13"
val mockkVersion = "1.13.14"
val lang3Version = "3.17.0"
val shedlockVersion = "6.0.2"
val confluentVersion = "7.5.1"
val avroVersion = "1.12.0"
val jacksonVersion = "2.18.2"
val mockOauth2ServerVersion = "2.1.10"

dependencies {
	implementation("org.springframework.boot:spring-boot-starter")
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-configuration-processor")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-logging")
	implementation("org.springframework.boot:spring-boot-starter-data-jdbc")

	implementation("org.springframework.retry:spring-retry")
	implementation("org.springframework:spring-aspects")

	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlin:kotlin-stdlib")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
	implementation("com.squareup.okhttp3:okhttp:$okhttp3Version")
	implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
	implementation("org.postgresql:postgresql")

	implementation("io.micrometer:micrometer-registry-prometheus")
	implementation("no.nav.common:log:$commonVersion")
	implementation("no.nav.common:token-client:$commonVersion")
	implementation("no.nav.common:rest:$commonVersion")
	implementation("no.nav.common:job:$commonVersion")
	implementation("no.nav.common:kafka:$commonVersion")

	implementation("io.confluent:kafka-avro-serializer:$confluentVersion")
	implementation("org.apache.avro:avro:$avroVersion")

	implementation("no.nav.poao-tilgang:client:$poaoTilgangVersion")

	implementation("org.apache.commons:commons-lang3:$lang3Version")

	implementation("no.nav.security:token-validation-spring:$tokenSupportVersion")

	implementation("net.javacrumbs.shedlock:shedlock-spring:$shedlockVersion")
	implementation("net.javacrumbs.shedlock:shedlock-provider-jdbc-template:$shedlockVersion")

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
		jvmTarget = "21"
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}
