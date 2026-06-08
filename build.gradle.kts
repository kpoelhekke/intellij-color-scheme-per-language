import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.DetektCreateBaselineTask
import org.jetbrains.changelog.Changelog
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.intellij.platform")
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

changelog {
    // Drive getChangelog/patchChangelog off the injected version, not the static project version.
    version = pluginVersion
    // CHANGELOG.md uses flat bullet lists, so disable the default grouped sections.
    groups.empty()
    repositoryUrl = providers.gradleProperty("pluginRepositoryUrl")
}

// Render the matching CHANGELOG.md section eagerly to a String. Resolving it here (rather than in a
// lazy provider that captures the `changelog` extension) keeps the patchPluginXml task config-cache safe.
val changeNotesHtml = with(changelog) {
    renderItem(
        (getOrNull(pluginVersion.get()) ?: getUnreleased())
            .withHeader(false)
            .withEmptySections(false),
        Changelog.OutputType.HTML,
    )
}

intellijPlatform {
    pluginConfiguration {
        version = pluginVersion
        changeNotes = changeNotesHtml
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
