
import org.apache.avro.tool.IdlTool
import org.apache.avro.tool.SpecificCompilerTool

plugins {
    // Du trenger ikke bruke apply false her — `buildSrc` håndterer Kotlin selv.
    // Men det skader heller ikke.
    kotlin("jvm") apply false
}

val avroSchemasDir = layout.projectDirectory.dir("src/main/avro")
val avroProtocolOutputDir = layout.buildDirectory.dir("generated/avro/protocols")
val avroCodeGenerationDir = layout.buildDirectory.dir("generated/avro/java")

// ✅ La Gradle vite at disse katalogene er del av main source set
sourceSets.named("main") {
    java.srcDir(avroCodeGenerationDir)
}

// ✅ Bruk en type-sikker task med deklarative inputs/outputs (bedre caching)
tasks.register("generateAvroJava") {
    group = "codegen"
    description = "Genererer Avro Java-klasser fra .avdl-filer"

    inputs.dir(avroSchemasDir)
    outputs.dir(avroCodeGenerationDir)

    doLast {
        val schemaDir = avroSchemasDir.asFile
        val protocolDir = avroProtocolOutputDir.get().asFile
        val outputDir = avroCodeGenerationDir.get().asFile

        // --- AVDL → AVPR ---
        schemaDir
            .walkTopDown()
            .filter { it.isFile && it.extension == "avdl" }
            .forEach { idlFile ->
                val outputFile = File(protocolDir, "${idlFile.nameWithoutExtension}.avpr")
                outputFile.parentFile.mkdirs()

                logger.lifecycle("Generating protocol from ${idlFile.name}")

                IdlTool().run(
                    System.`in`,
                    System.out,
                    System.err,
                    listOf(idlFile.absolutePath, outputFile.absolutePath),
                )
            }

        // --- AVPR → Java ---
        protocolDir
            .walkTopDown()
            .filter { it.isFile && it.extension == "avpr" }
            .forEach { protocolFile ->
                logger.lifecycle("Generating Java classes from ${protocolFile.name}")

                SpecificCompilerTool().run(
                    System.`in`,
                    System.out,
                    System.err,
                    listOf(
                        "protocol",
                        protocolFile.absolutePath,
                        outputDir.absolutePath,
                        "-encoding",
                        "UTF-8",
                        "-string",
                        "-fieldVisibility",
                        "private",
                        "-noSetters",
                    ),
                )
            }
    }
}

// ✅ Kjør kodegenerering automatisk før kompilering
tasks.named("compileJava").configure {
    dependsOn("generateAvroJava")
}
tasks.named("compileKotlin").configure {
    dependsOn("generateAvroJava")
}
