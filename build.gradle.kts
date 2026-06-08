import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.DetektCreateBaselineTask
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
