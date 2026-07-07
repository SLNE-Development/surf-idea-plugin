plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.4.0"
    id("org.jetbrains.intellij.platform") version "2.17.0"
}

group = "dev.slne.surf.idea"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
    maven("https://repo.slne.dev/repository/maven-public/") { name = "maven-public" }
}

// Read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
dependencies {
    intellijPlatform {
        intellijIdea("2026.1.4")
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)

        bundledPlugin("org.jetbrains.kotlin")

        pluginVerifier()
        zipSigner()
    }

    implementation("com.squareup:kotlinpoet:2.3.0")
}

intellijPlatform {
    instrumentCode = false

    pluginConfiguration {
        name = "Surf Framework Support"
        version = project.version.toString()
        description = """
            IntelliJ IDEA plugin providing inspections, code generation, and framework detection
            for the Surf ecosystem: surf-api, surf-redis, and surf-database-r2dbc.
        """.trimIndent()


        ideaVersion {
            sinceBuild = "262.8665.81"
        }

        changeNotes = """
            Initial version
        """.trimIndent()
    }

    pluginVerification {
        ides {
            recommended()
        }
    }
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }

    buildSearchableOptions {
        enabled = false
    }

    prepareSandbox {
        doLast {
            val sandboxPathsFile = sandboxConfigDirectory.file("disabled_plugins.txt").get().asFile
            sandboxPathsFile.writeText( // <- a list of plugin IDs
                """
                kubernetes
                """.trimIndent()
            )
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        freeCompilerArgs.set(listOf("-Xcontext-parameters"))
        optIn.add("org.jetbrains.kotlin.analysis.api.permissions.KaAllowProhibitedAnalyzeFromWriteAction")
    }
}
