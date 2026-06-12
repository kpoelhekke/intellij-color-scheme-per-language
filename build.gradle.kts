import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.DetektCreateBaselineTask
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.intellij.platform")
    // Applied only for the markdownToHTML util; CHANGELOG.md itself is owned by Release Please.
    id("org.jetbrains.changelog")
    id("io.gitlab.arturbosch.detekt")
}

dependencies {
    testImplementation("junit:junit:4.13.2")

    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.23.8")

    // IntelliJ Platform Gradle Plugin Dependencies Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html
    intellijPlatform {
        intellijIdea("2025.2.6.2")
        testFramework(TestFrameworkType.Platform)
    }
}

val pluginVersion = providers.gradleProperty("pluginVersion")

intellijPlatform {
    pluginConfiguration {
        version = pluginVersion

        // The Marketplace listing is the README section between the description markers,
        // so user-facing docs live in one place.
        description = providers.fileContents(layout.projectDirectory.file("README.md")).asText.map {
            val start = "<!-- Plugin description -->"
            val end = "<!-- Plugin description end -->"
            with(it.lines()) {
                if (!containsAll(listOf(start, end))) {
                    throw GradleException("Plugin description markers not found in README.md")
                }
                subList(indexOf(start) + 1, indexOf(end)).joinToString("\n")
            }.let(::markdownToHTML)
        }
        // Release notes are produced by Release Please; the publish workflow passes the GitHub
        // release body in via CHANGE_NOTES. Local builds without it get empty change notes.
        changeNotes = providers.environmentVariable("CHANGE_NOTES")
            .filter(String::isNotBlank)
            .map { markdownToHTML(it) }
            .orElse("")
    }

    // Signs only when the certificate env vars are non-blank; otherwise the release publishes unsigned.
    // Unset GitHub secrets surface as empty strings, so blanks are filtered to absent providers.
    signing {
        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN").filter(String::isNotBlank)
        privateKey = providers.environmentVariable("PRIVATE_KEY").filter(String::isNotBlank)
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD").filter(String::isNotBlank)
    }

    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
        // Derive the release channel from a tag suffix: 0.4.0 -> "default" (stable),
        // 0.4.0-beta.1 -> "beta", 0.4.0-eap.2 -> "eap", etc.
        channels = pluginVersion.map { version ->
            listOf(version.substringAfter('-', "").substringBefore('.').ifEmpty { "default" })
        }
    }

    pluginVerification {
        ides {
            recommended()
        }
    }
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(files("config/detekt/detekt.yml"))
    // Default `detekt`/`detektMain`/`detektTest` tasks stay read-only; use `detektFormat` to rewrite.
    autoCorrect = false
}

tasks.withType<Detekt>().configureEach {
    jvmTarget = "21"
    reports {
        html.required.set(true)
        sarif.required.set(true)
        xml.required.set(false)
        txt.required.set(false)
        md.required.set(false)
    }
}

tasks.withType<DetektCreateBaselineTask>().configureEach {
    jvmTarget = "21"
}

// Auto-correcting entry point. Formatting rules don't need type resolution, so no classpath is wired here.
val detektFormat by tasks.registering(Detekt::class) {
    description = "Runs detekt with auto-correction enabled."
    group = "formatting"
    autoCorrect = true
    buildUponDefaultConfig = true
    config.setFrom(files("config/detekt/detekt.yml"))
    setSource(files("src/main/kotlin", "src/test/kotlin"))
    include("**/*.kt", "**/*.kts")
}

// Enforce on `check`/`build` using the type-resolution-aware source-set tasks.
tasks.named("check") {
    dependsOn("detektMain", "detektTest")
}
