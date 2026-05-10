plugins {
    kotlin("jvm")
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.ktlint)
}

repositories {
    mavenCentral()
    maven { url = uri("https://github-package-registry-mirror.gc.nav.no/cached/maven-release") }
    maven { url = uri("https://packages.confluent.io/maven/") }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-logging")
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    implementation("org.springframework.boot:spring-boot-flyway")
    implementation("org.springframework.boot:spring-boot-restclient")

    implementation(libs.tools.jackson.module.kotlin)
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("org.postgresql:postgresql")

    implementation("io.micrometer:micrometer-registry-prometheus")

    implementation(libs.nav.common.log)
    implementation(libs.nav.common.token.client)
    implementation(libs.nav.common.rest)
    implementation(libs.nav.common.job)
    implementation(libs.nav.common.kafka)

    implementation(libs.logstash.encoder)
    implementation(libs.kafka.avro.serializer)

    implementation(libs.poao.tilgang.client)

    implementation(libs.token.validation.spring)

    implementation(libs.shedlock.spring)
    implementation(libs.shedlock.provider.jdbc.template)

    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "com.vaadin.external.google", module = "android-json")
    }
    testImplementation("org.springframework.boot:spring-boot-data-jdbc-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.springframework.boot:spring-boot-resttestclient")
    testImplementation("org.springframework.boot:spring-boot-restclient-test")

    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.assertions.json)

    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.kafka)

    testImplementation(libs.mockk)
    testImplementation(libs.springmockk)
    testImplementation(libs.mock.oauth2.server)
    testImplementation(libs.amt.lib.testing)
}

kotlin {
    jvmToolchain(25)
    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xjsr305=strict",
            "-Xannotation-default-target=param-property",
            "-Xwarning-level=IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE:disabled",
        )
    }
}

ktlint {
    version = libs.versions.ktlint.cli.version.get()
}

tasks.register<GenerateAvroTask>("generateAvroJava") {
    description = "Genererer Java-klasser fra Avro-skjemaer"
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
