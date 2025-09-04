plugins {
    val kotlinVersion = "2.2.10"

    id("org.springframework.boot") version "3.5.5"
    id("io.spring.dependency-management") version "1.1.7"
    id("com.github.davidmc24.gradle.plugin.avro") version "1.9.1"
    kotlin("plugin.serialization") version kotlinVersion
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.spring") version kotlinVersion
    id("org.jlleitschuh.gradle.ktlint") version "13.1.0"
}

group = "no.nav.amt-person-service"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_21

repositories {
    mavenCentral()
    maven { url = uri("https://github-package-registry-mirror.gc.nav.no/cached/maven-release") }
    maven { url = uri("https://packages.confluent.io/maven/") }
}

val commonVersion = "3.2025.09.03_08.33-728ff4acbfdb"
val okhttp3Version = "5.1.0"
val kotestVersion = "5.9.1"
val poaoTilgangVersion = "2025.07.04_08.56-814fa50f6740"
val testcontainersVersion = "1.21.3"
val tokenSupportVersion = "5.0.36"
val mockkVersion = "1.14.5"
val lang3Version = "3.18.0"
val shedlockVersion = "6.10.0"
val confluentVersion = "8.0.0"
val avroVersion = "1.12.0"
val jacksonVersion = "2.20.0"
val mockOauth2ServerVersion = "2.2.1"
val logstashEncoderVersion = "8.1"
val ktLintVersion = "1.6.0"

dependencyManagement {
    imports {
        mavenBom("org.testcontainers:testcontainers-bom:$testcontainersVersion")
    }
}

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

    implementation("net.logstash.logback:logstash-logback-encoder:$logstashEncoderVersion")

    implementation("io.confluent:kafka-avro-serializer:$confluentVersion")
    implementation("org.apache.avro:avro:$avroVersion")

    implementation("no.nav.poao-tilgang:client:$poaoTilgangVersion")

    implementation("org.apache.commons:commons-lang3:$lang3Version")

    implementation("no.nav.security:token-validation-spring:$tokenSupportVersion")

    implementation("net.javacrumbs.shedlock:shedlock-spring:$shedlockVersion")
    implementation("net.javacrumbs.shedlock:shedlock-provider-jdbc-template:$shedlockVersion")

    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.kotest:kotest-assertions-core-jvm:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-json-jvm:$kotestVersion")
    testImplementation("com.squareup.okhttp3:mockwebserver:$okhttp3Version")

    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:kafka")
    testImplementation("io.mockk:mockk-jvm:$mockkVersion")
    testImplementation("no.nav.security:mock-oauth2-server:$mockOauth2ServerVersion")
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xjsr305=strict",
            "-Xannotation-default-target=param-property",
            "-Xwarning-level=IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE:disabled",
        )
    }
}

ktlint {
    version = ktLintVersion
}

tasks.compileKotlin {
    dependsOn("generateAvroJava")
}

tasks.runKtlintCheckOverMainSourceSet {
    dependsOn("generateAvroJava")
}

tasks.runKtlintCheckOverTestSourceSet {
    dependsOn("generateTestAvroJava")
}

tasks.jar {
    enabled = false
}

tasks.test {
    useJUnitPlatform()
    jvmArgs(
        "-Xshare:off",
        "-XX:+EnableDynamicAgentLoading",
    )
}
