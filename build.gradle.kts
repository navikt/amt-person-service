plugins {
    val kotlinVersion = "2.2.21"

    kotlin("jvm") // versjon settes i buildSrc
    id("org.springframework.boot") version "3.5.7"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("plugin.serialization") version kotlinVersion
    kotlin("plugin.spring") version kotlinVersion
    id("org.jlleitschuh.gradle.ktlint") version "14.0.1"
}

group = "no.nav.amt-person-service"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_21

repositories {
    mavenCentral()
    maven { url = uri("https://github-package-registry-mirror.gc.nav.no/cached/maven-release") }
    maven { url = uri("https://packages.confluent.io/maven/") }
}

val commonVersion = "3.2025.10.10_08.21-bb7c7830d93c"
val okhttp3Version = "5.3.2"
val kotestVersion = "6.0.7"
val poaoTilgangVersion = "2025.11.03_13.40-18456d0598be"
val testcontainersVersion = "2.0.2"
val tokenSupportVersion = "5.0.39"
val mockkVersion = "1.14.6"
val lang3Version = "3.20.0"
val shedlockVersion = "7.2.1"
val confluentVersion = "8.1.1"
val jacksonVersion = "2.20.1"
val mockOauth2ServerVersion = "3.0.1"
val logstashEncoderVersion = "9.0"
val ktLintVersion = "1.6.0"

dependencyManagement {
    imports {
        mavenBom("org.testcontainers:testcontainers-bom:$testcontainersVersion")
    }
}

// fjernes ved neste release av org.apache.kafka:kafka-clients
configurations.configureEach {
    resolutionStrategy {
        capabilitiesResolution {
            withCapability("org.lz4:lz4-java") {
                select(candidates.first { (it.id as ModuleComponentIdentifier).group == "at.yawk.lz4" })
            }
        }
    }
}

dependencies {
    implementation("at.yawk.lz4:lz4-java:1.10.1") // fjernes ved neste release av org.apache.kafka:kafka-clients
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

tasks.register<GenerateAvroTask>("generateAvroJava") {
    avroSchemasDir = layout.projectDirectory.dir("src/main/avro")
    avroCodeGenerationDir = layout.buildDirectory.dir("generated/avro/java")
}

sourceSets.named("main") {
    java.srcDir(
        tasks
            .named<GenerateAvroTask>("generateAvroJava")
            .flatMap { it.avroCodeGenerationDir },
    )
}

tasks.named("compileKotlin") {
    dependsOn("generateAvroJava")
}

tasks.named("runKtlintCheckOverMainSourceSet") {
    dependsOn("generateAvroJava")
}

tasks.named("runKtlintFormatOverMainSourceSet") {
    dependsOn("generateAvroJava")
}

tasks.named("jar") {
    enabled = false
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    jvmArgs(
        "-Xshare:off",
        "-XX:+EnableDynamicAgentLoading",
    )
}
