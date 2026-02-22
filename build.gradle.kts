import org.jetbrains.intellij.platform.gradle.tasks.BuildSearchableOptionsTask

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.3.0"
    id("org.jetbrains.intellij.platform") version "2.11.0"
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.0"
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
        intellijIdea("2025.3.2")
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)

        composeUI()
        bundledPlugin("org.jetbrains.kotlin")

        pluginVerifier()
        zipSigner()
    }
}

intellijPlatform {
    pluginConfiguration {
        name = "Surf Framework Support"
        version = project.version.toString()
        description = """
            IntelliJ IDEA plugin providing inspections, code generation, and framework detection
            for the Surf ecosystem: surf-api, surf-redis, and surf-database-r2dbc.
        """.trimIndent()


        ideaVersion {
//            sinceBuild = "252.25557"
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
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}
