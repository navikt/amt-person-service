import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	id("org.springframework.boot") version "3.0.5"
	id("io.spring.dependency-management") version "1.1.0"
	kotlin("jvm") version "1.7.22"
	kotlin("plugin.spring") version "1.7.22"
}

group = "no.nav.amt-person-service"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
	mavenCentral()
	maven { setUrl("https://jitpack.io") }
}

val commonVersion = "3.2023.03.22_12.48-00fcbdc8f455"
val okhttp3Version = "4.10.0"
val kotestVersion = "5.5.5"
val poaoTilgangVersion = "2023.04.12_11.17-8706c9ad4b87"
val testcontainersVersion = "1.18.0"
val tokenSupportVersion = "3.0.10"
val mockkVersion = "1.13.5"
val lang3Version = "3.12.0"
val shedlockVersion = "5.2.0"

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
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("com.squareup.okhttp3:okhttp:$okhttp3Version")
	implementation("org.flywaydb:flyway-core")

	implementation("io.micrometer:micrometer-registry-prometheus")
	implementation("com.github.navikt.common-java-modules:log:$commonVersion")
	implementation("com.github.navikt.common-java-modules:token-client:$commonVersion")
	implementation("com.github.navikt.common-java-modules:rest:$commonVersion")
	implementation("com.github.navikt.common-java-modules:job:$commonVersion")

	implementation("com.github.navikt.poao-tilgang:client:$poaoTilgangVersion")

	implementation("org.apache.commons:commons-lang3:$lang3Version")
	implementation("no.nav.security:token-validation-core:$tokenSupportVersion")
	implementation("net.javacrumbs.shedlock:shedlock-spring:$shedlockVersion")

	runtimeOnly("org.postgresql:postgresql")

	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("io.kotest:kotest-assertions-core-jvm:$kotestVersion")
	testImplementation("io.kotest:kotest-assertions-json-jvm:$kotestVersion")
	testImplementation("com.squareup.okhttp3:mockwebserver:$okhttp3Version")
	testImplementation("org.testcontainers:testcontainers:$testcontainersVersion")
	testImplementation("org.testcontainers:postgresql:$testcontainersVersion")
	testImplementation("io.mockk:mockk:$mockkVersion")
}

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
