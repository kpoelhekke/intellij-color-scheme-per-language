# Editor Scheme by Language Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build an IntelliJ IDEA plugin that applies installed editor color schemes to the active editor based on configured `Language` or `FileType` rules.

**Architecture:** Start from the official JetBrains IntelliJ Platform Plugin Template, then add focused Kotlin units for persistent settings, pure rule matching, IntelliJ API adaptation, editor activation listening, and a Tools settings page. Runtime work stays limited to active-editor events and uses local editor-only scheme assignment; implementation stops if that API path is not viable.

**Tech Stack:** Kotlin, Gradle Kotlin DSL, IntelliJ Platform Gradle Plugin, IntelliJ Platform SDK, Swing/Kotlin UI DSL, JUnit Platform tests.

---

## References

- Official template: `JetBrains/intellij-platform-plugin-template`
- JetBrains docs: `Settings Guide`, especially `com.intellij.applicationConfigurable` under `parentId="tools"`
- JetBrains docs: `Settings Tutorial`, especially `PersistentStateComponent`
- JetBrains docs: `Listeners`, especially declarative listener registration for lazy listener creation
- JetBrains docs: `Editors`, especially `FileEditorManagerListener` for selection changes
- JetBrains docs: `Color Scheme Management`, for editor scheme terminology and installed scheme behavior

## File Structure

Create or keep the official template files:

- `settings.gradle.kts`: Gradle plugin management and project name
- `build.gradle.kts`: Kotlin, IntelliJ Platform Gradle Plugin, dependencies, IntelliJ IDEA target
- `gradle.properties`: plugin metadata and official template properties
- `src/main/resources/META-INF/plugin.xml`: plugin ID, name, settings configurable, project listener
- `src/main/resources/messages/EditorSchemeByLanguageBundle.properties`: UI text
- `src/main/kotlin/dev/appelflap/editorschemebylanguage/EditorSchemeByLanguageBundle.kt`: message bundle accessor

Create feature files:

- `src/main/kotlin/dev/appelflap/editorschemebylanguage/settings/EditorSchemeSettingsState.kt`: persisted state model and application service
- `src/main/kotlin/dev/appelflap/editorschemebylanguage/settings/EditorSchemeConfigurable.kt`: Settings | Tools configurable controller
- `src/main/kotlin/dev/appelflap/editorschemebylanguage/settings/EditorSchemeSettingsPanel.kt`: settings UI panel and table
- `src/main/kotlin/dev/appelflap/editorschemebylanguage/settings/RuleTargetChooserDialog.kt`: searchable chooser for `Language` and `FileType`
- `src/main/kotlin/dev/appelflap/editorschemebylanguage/model/RuleTargetKind.kt`: `Language` vs `FileType`
- `src/main/kotlin/dev/appelflap/editorschemebylanguage/model/SchemeRule.kt`: one mapping row
- `src/main/kotlin/dev/appelflap/editorschemebylanguage/model/EditorSchemeContext.kt`: resolved editor metadata
- `src/main/kotlin/dev/appelflap/editorschemebylanguage/model/SchemeRef.kt`: installed scheme reference
- `src/main/kotlin/dev/appelflap/editorschemebylanguage/matching/EditorSchemeMatcher.kt`: pure precedence and fallback logic
- `src/main/kotlin/dev/appelflap/editorschemebylanguage/platform/EditorSchemePlatform.kt`: IntelliJ-facing interface
- `src/main/kotlin/dev/appelflap/editorschemebylanguage/platform/IntellijEditorSchemePlatform.kt`: IntelliJ API adapter
- `src/main/kotlin/dev/appelflap/editorschemebylanguage/runtime/EditorSchemeSelectionListener.kt`: active editor listener

Create test files:

- `src/test/kotlin/dev/appelflap/editorschemebylanguage/matching/EditorSchemeMatcherTest.kt`
- `src/test/kotlin/dev/appelflap/editorschemebylanguage/settings/EditorSchemeSettingsStateTest.kt`
- `src/test/kotlin/dev/appelflap/editorschemebylanguage/settings/EditorSchemeSettingsPanelValidationTest.kt`
- `src/test/kotlin/dev/appelflap/editorschemebylanguage/runtime/EditorSchemeSelectionListenerTest.kt`

## Task 1: Bootstrap Official Plugin Template

**Files:**

- Create: all official template files copied from `JetBrains/intellij-platform-plugin-template`
- Modify: `settings.gradle.kts`
- Modify: `gradle.properties`
- Modify: `build.gradle.kts`
- Modify: `src/main/resources/META-INF/plugin.xml`
- Modify: `src/main/resources/messages/EditorSchemeByLanguageBundle.properties`
- Create: `src/main/kotlin/dev/appelflap/editorschemebylanguage/EditorSchemeByLanguageBundle.kt`
- Delete: template sample classes that are unrelated to this plugin

- [ ] **Step 1: Initialize git because the workspace is currently not a repository**

Run:

```bash
git init
```

Expected: output includes `Initialized empty Git repository`.

- [ ] **Step 2: Copy the official JetBrains template into the workspace**

Run:

```bash
git clone --depth 1 https://github.com/JetBrains/intellij-platform-plugin-template /tmp/intellij-platform-plugin-template
```

Expected: clone succeeds and `/tmp/intellij-platform-plugin-template/build.gradle.kts` exists.

Run:

```bash
rsync -a --exclude .git --exclude .github /tmp/intellij-platform-plugin-template/ ./
```

Expected: `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`, and `src/main/resources/META-INF/plugin.xml` exist in the workspace.

- [ ] **Step 3: Configure project identity**

Edit `settings.gradle.kts` so the project name is:

```kotlin
rootProject.name = "editor-scheme-by-language"
```

Edit `gradle.properties` so plugin metadata contains these values, preserving other official template defaults:

```properties
pluginGroup = dev.appelflap
pluginName = Editor Scheme by Language
pluginRepositoryUrl = https://github.com/appelflap/editor-scheme-by-language
pluginDescription = Applies installed editor color schemes to active IntelliJ IDEA editors based on configured Language or FileType rules.
pluginVersion = 0.1.0
```

- [ ] **Step 4: Configure plugin manifest**

Replace template sample extensions in `src/main/resources/META-INF/plugin.xml` with:

```xml
<idea-plugin>
    <id>dev.appelflap.editor-scheme-by-language</id>
    <name>Editor Scheme by Language</name>
    <vendor>appelflap</vendor>

    <depends>com.intellij.modules.platform</depends>

    <extensions defaultExtensionNs="com.intellij">
        <applicationConfigurable
                parentId="tools"
                instance="dev.appelflap.editorschemebylanguage.settings.EditorSchemeConfigurable"
                id="dev.appelflap.editorschemebylanguage.settings.EditorSchemeConfigurable"
                displayName="Editor Scheme by Language"/>
    </extensions>

    <projectListeners>
        <listener
                class="dev.appelflap.editorschemebylanguage.runtime.EditorSchemeSelectionListener"
                topic="com.intellij.openapi.fileEditor.FileEditorManagerListener"/>
    </projectListeners>
</idea-plugin>
```

- [ ] **Step 5: Add message bundle**

Replace or create `src/main/resources/messages/EditorSchemeByLanguageBundle.properties`:

```properties
settings.display.name=Editor Scheme by Language
settings.enable=Enable automatic editor scheme switching
settings.rules.title=Rules
settings.column.target=Target
settings.column.scheme=Scheme
settings.add=Add
settings.remove=Remove
settings.validation.duplicate=Each target can only be configured once.
settings.validation.missing.scheme=One or more rules reference a missing editor color scheme.
dialog.target.title=Choose Language or File Type
dialog.target.search=Search
```

Create `src/main/kotlin/dev/appelflap/editorschemebylanguage/EditorSchemeByLanguageBundle.kt`:

```kotlin
package dev.appelflap.editorschemebylanguage

import com.intellij.DynamicBundle
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey

@NonNls
private const val BUNDLE = "messages.EditorSchemeByLanguageBundle"

object EditorSchemeByLanguageBundle : DynamicBundle(BUNDLE) {
    fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any): String =
        getMessage(key, *params)
}
```

- [ ] **Step 6: Remove template sample source files**

Run:

```bash
find src/main/kotlin -type f | grep -v EditorSchemeByLanguageBundle.kt
```

Expected: only template sample files are listed.

Delete template sample files under `src/main/kotlin/org/jetbrains/plugins/template` and template sample tests under `src/test/kotlin/org/jetbrains/plugins/template`.

- [ ] **Step 7: Verify bootstrap compiles far enough to reveal missing feature classes**

Run:

```bash
./gradlew test
```

Expected: build fails because `EditorSchemeConfigurable` and `EditorSchemeSelectionListener` do not exist yet.

- [ ] **Step 8: Commit bootstrap**

Run:

```bash
git add .
git commit -m "chore: bootstrap IntelliJ plugin template"
```

Expected: commit succeeds.

## Task 2: Validate Local Editor Scheme Feasibility

**Files:**

- Create: `src/main/kotlin/dev/appelflap/editorschemebylanguage/platform/EditorSchemePlatform.kt`
- Create: `src/main/kotlin/dev/appelflap/editorschemebylanguage/platform/IntellijEditorSchemePlatform.kt`
- Test: compile through `./gradlew test`

- [ ] **Step 1: Create the platform interface**

Create `src/main/kotlin/dev/appelflap/editorschemebylanguage/platform/EditorSchemePlatform.kt`:

```kotlin
package dev.appelflap.editorschemebylanguage.platform

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.fileTypes.FileType
import com.intellij.lang.Language

data class PlatformEditorContext(
    val language: Language?,
    val fileType: FileType?,
)

interface EditorSchemePlatform {
    fun contextFor(editor: Editor): PlatformEditorContext?
    fun currentGlobalScheme(): EditorColorsScheme
    fun findScheme(name: String): EditorColorsScheme?
    fun installedSchemes(): List<EditorColorsScheme>
    fun applySchemeToEditor(editor: Editor, scheme: EditorColorsScheme): Boolean
}
```

- [ ] **Step 2: Create the IntelliJ adapter**

Create `src/main/kotlin/dev/appelflap/editorschemebylanguage/platform/IntellijEditorSchemePlatform.kt`:

```kotlin
package dev.appelflap.editorschemebylanguage.platform

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager

class IntellijEditorSchemePlatform(
    private val project: Project,
) : EditorSchemePlatform {
    override fun contextFor(editor: Editor): PlatformEditorContext? {
        val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.document)
        val virtualFile = FileDocumentManager.getInstance().getFile(editor.document)
        val language = psiFile?.language
        val fileType = psiFile?.fileType ?: virtualFile?.fileType
        return if (language == null && fileType == null) null else PlatformEditorContext(language, fileType)
    }

    override fun currentGlobalScheme(): EditorColorsScheme =
        EditorColorsManager.getInstance().globalScheme

    override fun findScheme(name: String): EditorColorsScheme? =
        EditorColorsManager.getInstance().getScheme(name)

    override fun installedSchemes(): List<EditorColorsScheme> =
        EditorColorsManager.getInstance().allSchemes.toList()

    override fun applySchemeToEditor(editor: Editor, scheme: EditorColorsScheme): Boolean {
        val editorEx = editor as? EditorEx ?: return false
        ApplicationManager.getApplication().invokeLater {
            editorEx.colorsScheme = scheme
            editorEx.component.repaint()
        }
        return true
    }
}
```

- [ ] **Step 3: Compile to validate the per-editor API**

Run:

```bash
./gradlew test
```

Expected: compilation succeeds for `editorEx.colorsScheme = scheme`, then fails only for missing classes referenced by `plugin.xml`.

If compilation fails because `EditorEx.colorsScheme` cannot be assigned or no equivalent public local-editor assignment API exists, stop implementation and write `docs/platform-feasibility.md`:

```markdown
# Platform Feasibility

The IntelliJ Platform API available from the official template baseline does not expose a reliable public editor-instance color scheme assignment path for Editor Scheme by Language.

The plugin will not fall back to global IDE scheme switching because the product requirement is local editor-only scheme application.
```

Then commit the feasibility note and do not continue feature implementation.

- [ ] **Step 4: Commit feasibility adapter**

Run:

```bash
git add src/main/kotlin/dev/appelflap/editorschemebylanguage/platform
git commit -m "feat: validate editor-local scheme adapter"
```

Expected: commit succeeds.

## Task 3: Add Settings Model and Pure Matcher

**Files:**

- Create: `src/main/kotlin/dev/appelflap/editorschemebylanguage/model/RuleTargetKind.kt`
- Create: `src/main/kotlin/dev/appelflap/editorschemebylanguage/model/SchemeRule.kt`
- Create: `src/main/kotlin/dev/appelflap/editorschemebylanguage/model/EditorSchemeContext.kt`
- Create: `src/main/kotlin/dev/appelflap/editorschemebylanguage/model/SchemeRef.kt`
- Create: `src/main/kotlin/dev/appelflap/editorschemebylanguage/settings/EditorSchemeSettingsState.kt`
- Create: `src/main/kotlin/dev/appelflap/editorschemebylanguage/matching/EditorSchemeMatcher.kt`
- Test: `src/test/kotlin/dev/appelflap/editorschemebylanguage/matching/EditorSchemeMatcherTest.kt`
- Test: `src/test/kotlin/dev/appelflap/editorschemebylanguage/settings/EditorSchemeSettingsStateTest.kt`

- [ ] **Step 1: Write failing matcher tests**

Create `src/test/kotlin/dev/appelflap/editorschemebylanguage/matching/EditorSchemeMatcherTest.kt`:

```kotlin
package dev.appelflap.editorschemebylanguage.matching

import dev.appelflap.editorschemebylanguage.model.EditorSchemeContext
import dev.appelflap.editorschemebylanguage.model.RuleTargetKind
import dev.appelflap.editorschemebylanguage.model.SchemeRef
import dev.appelflap.editorschemebylanguage.model.SchemeRule
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EditorSchemeMatcherTest {
    private val default = SchemeRef("Default")
    private val kotlin = SchemeRef("Kotlin Scheme")
    private val typescript = SchemeRef("TypeScript Scheme")

    @Test
    fun `language rule wins over file type rule`() {
        val matcher = EditorSchemeMatcher(
            installedSchemeNames = setOf(default.name, kotlin.name, typescript.name),
            defaultScheme = default,
        )

        val result = matcher.resolve(
            enabled = true,
            rules = listOf(
                SchemeRule(RuleTargetKind.FILE_TYPE, "Kotlin", "Kotlin", typescript.name),
                SchemeRule(RuleTargetKind.LANGUAGE, "kotlin", "Kotlin", kotlin.name),
            ),
            context = EditorSchemeContext(
                languageId = "kotlin",
                languageDisplayName = "Kotlin",
                fileTypeId = "Kotlin",
                fileTypeDisplayName = "Kotlin",
            ),
        )

        assertEquals(kotlin, result)
    }

    @Test
    fun `file type rule is used when language rule is absent`() {
        val matcher = EditorSchemeMatcher(
            installedSchemeNames = setOf(default.name, typescript.name),
            defaultScheme = default,
        )

        val result = matcher.resolve(
            enabled = true,
            rules = listOf(SchemeRule(RuleTargetKind.FILE_TYPE, "TypeScript JSX", "TypeScript JSX", typescript.name)),
            context = EditorSchemeContext(
                languageId = "TypeScript JSX",
                languageDisplayName = "TypeScript JSX",
                fileTypeId = "TypeScript JSX",
                fileTypeDisplayName = "TypeScript JSX",
            ),
        )

        assertEquals(typescript, result)
    }

    @Test
    fun `default scheme is used when disabled`() {
        val matcher = EditorSchemeMatcher(
            installedSchemeNames = setOf(default.name, kotlin.name),
            defaultScheme = default,
        )

        val result = matcher.resolve(
            enabled = false,
            rules = listOf(SchemeRule(RuleTargetKind.LANGUAGE, "kotlin", "Kotlin", kotlin.name)),
            context = EditorSchemeContext("kotlin", "Kotlin", "Kotlin", "Kotlin"),
        )

        assertEquals(default, result)
    }

    @Test
    fun `default scheme is used when mapped scheme is missing`() {
        val matcher = EditorSchemeMatcher(
            installedSchemeNames = setOf(default.name),
            defaultScheme = default,
        )

        val result = matcher.resolve(
            enabled = true,
            rules = listOf(SchemeRule(RuleTargetKind.LANGUAGE, "kotlin", "Kotlin", "Missing Scheme")),
            context = EditorSchemeContext("kotlin", "Kotlin", "Kotlin", "Kotlin"),
        )

        assertEquals(default, result)
    }
}
```

Run:

```bash
./gradlew test --tests dev.appelflap.editorschemebylanguage.matching.EditorSchemeMatcherTest
```

Expected: tests fail because model and matcher classes do not exist.

- [ ] **Step 2: Add model classes**

Create `src/main/kotlin/dev/appelflap/editorschemebylanguage/model/RuleTargetKind.kt`:

```kotlin
package dev.appelflap.editorschemebylanguage.model

enum class RuleTargetKind {
    LANGUAGE,
    FILE_TYPE,
}
```

Create `src/main/kotlin/dev/appelflap/editorschemebylanguage/model/SchemeRule.kt`:

```kotlin
package dev.appelflap.editorschemebylanguage.model

data class SchemeRule(
    var targetKind: RuleTargetKind = RuleTargetKind.LANGUAGE,
    var targetId: String = "",
    var targetDisplayName: String = "",
    var schemeName: String = "",
) {
    fun targetKey(): String = "${targetKind.name}:$targetId"
}
```

Create `src/main/kotlin/dev/appelflap/editorschemebylanguage/model/EditorSchemeContext.kt`:

```kotlin
package dev.appelflap.editorschemebylanguage.model

data class EditorSchemeContext(
    val languageId: String?,
    val languageDisplayName: String?,
    val fileTypeId: String?,
    val fileTypeDisplayName: String?,
)
```

Create `src/main/kotlin/dev/appelflap/editorschemebylanguage/model/SchemeRef.kt`:

```kotlin
package dev.appelflap.editorschemebylanguage.model

data class SchemeRef(
    val name: String,
)
```

- [ ] **Step 3: Add settings state service**

Create `src/main/kotlin/dev/appelflap/editorschemebylanguage/settings/EditorSchemeSettingsState.kt`:

```kotlin
package dev.appelflap.editorschemebylanguage.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil
import dev.appelflap.editorschemebylanguage.model.SchemeRule

@State(
    name = "dev.appelflap.editorschemebylanguage.EditorSchemeSettingsState",
    storages = [Storage("EditorSchemeByLanguage.xml")],
)
class EditorSchemeSettingsState : PersistentStateComponent<EditorSchemeSettingsState.State> {
    data class State(
        var enabled: Boolean = true,
        var rules: MutableList<SchemeRule> = mutableListOf(),
    )

    private var settingsState = State()

    override fun getState(): State = settingsState

    override fun loadState(state: State) {
        XmlSerializerUtil.copyBean(state, settingsState)
    }

    fun update(enabled: Boolean, rules: List<SchemeRule>) {
        settingsState.enabled = enabled
        settingsState.rules = rules.map { it.copy() }.toMutableList()
    }

    companion object {
        fun getInstance(): EditorSchemeSettingsState =
            ApplicationManager.getApplication().getService(EditorSchemeSettingsState::class.java)
    }
}
```

- [ ] **Step 4: Add pure matcher**

Create `src/main/kotlin/dev/appelflap/editorschemebylanguage/matching/EditorSchemeMatcher.kt`:

```kotlin
package dev.appelflap.editorschemebylanguage.matching

import dev.appelflap.editorschemebylanguage.model.EditorSchemeContext
import dev.appelflap.editorschemebylanguage.model.RuleTargetKind
import dev.appelflap.editorschemebylanguage.model.SchemeRef
import dev.appelflap.editorschemebylanguage.model.SchemeRule

class EditorSchemeMatcher(
    private val installedSchemeNames: Set<String>,
    private val defaultScheme: SchemeRef,
) {
    fun resolve(
        enabled: Boolean,
        rules: List<SchemeRule>,
        context: EditorSchemeContext,
    ): SchemeRef {
        if (!enabled) return defaultScheme

        val languageRule = context.languageId?.let { id ->
            rules.firstOrNull { it.targetKind == RuleTargetKind.LANGUAGE && it.targetId == id }
        }
        val fileTypeRule = context.fileTypeId?.let { id ->
            rules.firstOrNull { it.targetKind == RuleTargetKind.FILE_TYPE && it.targetId == id }
        }
        val matchingRule = languageRule ?: fileTypeRule ?: return defaultScheme

        return if (matchingRule.schemeName in installedSchemeNames) {
            SchemeRef(matchingRule.schemeName)
        } else {
            defaultScheme
        }
    }
}
```

- [ ] **Step 5: Add persistence unit test**

Create `src/test/kotlin/dev/appelflap/editorschemebylanguage/settings/EditorSchemeSettingsStateTest.kt`:

```kotlin
package dev.appelflap.editorschemebylanguage.settings

import dev.appelflap.editorschemebylanguage.model.RuleTargetKind
import dev.appelflap.editorschemebylanguage.model.SchemeRule
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class EditorSchemeSettingsStateTest {
    @Test
    fun `state saves and reloads enabled flag and rules`() {
        val service = EditorSchemeSettingsState()
        service.update(
            enabled = false,
            rules = listOf(SchemeRule(RuleTargetKind.LANGUAGE, "kotlin", "Kotlin", "Kotlin Scheme")),
        )

        val reloaded = EditorSchemeSettingsState()
        reloaded.loadState(service.getState())

        assertFalse(reloaded.getState().enabled)
        assertEquals(
            listOf(SchemeRule(RuleTargetKind.LANGUAGE, "kotlin", "Kotlin", "Kotlin Scheme")),
            reloaded.getState().rules,
        )
    }
}
```

- [ ] **Step 6: Run tests**

Run:

```bash
./gradlew test --tests dev.appelflap.editorschemebylanguage.matching.EditorSchemeMatcherTest --tests dev.appelflap.editorschemebylanguage.settings.EditorSchemeSettingsStateTest
```

Expected: tests pass.

- [ ] **Step 7: Commit model and matcher**

Run:

```bash
git add src/main/kotlin/dev/appelflap/editorschemebylanguage/model src/main/kotlin/dev/appelflap/editorschemebylanguage/settings/EditorSchemeSettingsState.kt src/main/kotlin/dev/appelflap/editorschemebylanguage/matching src/test/kotlin/dev/appelflap/editorschemebylanguage
git commit -m "feat: add scheme rule matching"
```

Expected: commit succeeds.

## Task 4: Add Runtime Editor Activation Listener

**Files:**

- Modify: `src/main/kotlin/dev/appelflap/editorschemebylanguage/platform/IntellijEditorSchemePlatform.kt`
- Create: `src/main/kotlin/dev/appelflap/editorschemebylanguage/runtime/EditorSchemeSelectionListener.kt`
- Test: `src/test/kotlin/dev/appelflap/editorschemebylanguage/runtime/EditorSchemeSelectionListenerTest.kt`

- [ ] **Step 1: Write listener unit test with fake platform**

Create `src/test/kotlin/dev/appelflap/editorschemebylanguage/runtime/EditorSchemeSelectionListenerTest.kt`:

```kotlin
package dev.appelflap.editorschemebylanguage.runtime

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import dev.appelflap.editorschemebylanguage.model.RuleTargetKind
import dev.appelflap.editorschemebylanguage.model.SchemeRule
import dev.appelflap.editorschemebylanguage.platform.EditorSchemePlatform
import dev.appelflap.editorschemebylanguage.platform.PlatformEditorContext
import org.junit.jupiter.api.Assertions.assertEquals

class EditorSchemeSelectionListenerTest : BasePlatformTestCase() {
    fun testAppliesResolvedSchemeToActiveEditor() {
        val editor = myFixture.configureByText("sample.txt", "plain text").editor
        val colorsManager = EditorColorsManager.getInstance()
        val defaultScheme = colorsManager.globalScheme
        val platform = FakePlatform(defaultScheme)
        val listener = EditorSchemeSelectionHandler(
            platform = platform,
            enabled = { true },
            rules = { listOf(SchemeRule(RuleTargetKind.FILE_TYPE, PlainTextFileType.INSTANCE.name, "Plain Text", defaultScheme.name)) },
        )

        listener.applyForEditor(editor)

        assertEquals(defaultScheme, platform.appliedScheme)
    }

    private class FakePlatform(
        private val defaultScheme: EditorColorsScheme,
    ) : EditorSchemePlatform {
        var appliedScheme: EditorColorsScheme? = null

        override fun contextFor(editor: Editor): PlatformEditorContext? =
            PlatformEditorContext(language = null, fileType = PlainTextFileType.INSTANCE)

        override fun currentGlobalScheme(): EditorColorsScheme = defaultScheme

        override fun findScheme(name: String): EditorColorsScheme? =
            if (name == defaultScheme.name) defaultScheme else null

        override fun installedSchemes(): List<EditorColorsScheme> = listOf(defaultScheme)

        override fun applySchemeToEditor(editor: Editor, scheme: EditorColorsScheme): Boolean {
            appliedScheme = scheme
            return true
        }
    }
}
```

Run:

```bash
./gradlew test --tests dev.appelflap.editorschemebylanguage.runtime.EditorSchemeSelectionListenerTest
```

Expected: test fails because `EditorSchemeSelectionHandler` does not exist.

- [ ] **Step 2: Add handler and listener**

Create `src/main/kotlin/dev/appelflap/editorschemebylanguage/runtime/EditorSchemeSelectionListener.kt`:

```kotlin
package dev.appelflap.editorschemebylanguage.runtime

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.fileEditor.TextEditor
import dev.appelflap.editorschemebylanguage.matching.EditorSchemeMatcher
import dev.appelflap.editorschemebylanguage.model.EditorSchemeContext
import dev.appelflap.editorschemebylanguage.model.SchemeRef
import dev.appelflap.editorschemebylanguage.model.SchemeRule
import dev.appelflap.editorschemebylanguage.platform.EditorSchemePlatform
import dev.appelflap.editorschemebylanguage.platform.IntellijEditorSchemePlatform
import dev.appelflap.editorschemebylanguage.settings.EditorSchemeSettingsState

class EditorSchemeSelectionListener : FileEditorManagerListener {
    override fun selectionChanged(event: FileEditorManagerEvent) {
        val project = event.manager.project
        val textEditor = event.newEditor as? TextEditor ?: return
        val editor = textEditor.editor as? EditorEx ?: return
        val settings = EditorSchemeSettingsState.getInstance().state

        EditorSchemeSelectionHandler(
            platform = IntellijEditorSchemePlatform(project),
            enabled = { settings.enabled },
            rules = { settings.rules },
        ).applyForEditor(editor)
    }
}

class EditorSchemeSelectionHandler(
    private val platform: EditorSchemePlatform,
    private val enabled: () -> Boolean,
    private val rules: () -> List<SchemeRule>,
) {
    fun applyForEditor(editor: Editor): Boolean {
        val platformContext = platform.contextFor(editor) ?: return false
        val defaultScheme = platform.currentGlobalScheme()
        val matcher = EditorSchemeMatcher(
            installedSchemeNames = platform.installedSchemes().map { it.name }.toSet(),
            defaultScheme = SchemeRef(defaultScheme.name),
        )
        val schemeRef = matcher.resolve(
            enabled = enabled(),
            rules = rules(),
            context = EditorSchemeContext(
                languageId = platformContext.language?.id,
                languageDisplayName = platformContext.language?.displayName,
                fileTypeId = platformContext.fileType?.name,
                fileTypeDisplayName = platformContext.fileType?.displayName,
            ),
        )
        val scheme = platform.findScheme(schemeRef.name) ?: defaultScheme
        return platform.applySchemeToEditor(editor, scheme)
    }
}
```

- [ ] **Step 3: Run runtime tests**

Run:

```bash
./gradlew test --tests dev.appelflap.editorschemebylanguage.runtime.EditorSchemeSelectionListenerTest
```

Expected: tests pass.

- [ ] **Step 4: Run full test suite**

Run:

```bash
./gradlew test
```

Expected: all current tests pass.

- [ ] **Step 5: Commit runtime listener**

Run:

```bash
git add src/main/kotlin/dev/appelflap/editorschemebylanguage/runtime src/main/kotlin/dev/appelflap/editorschemebylanguage/platform src/test/kotlin/dev/appelflap/editorschemebylanguage/runtime
git commit -m "feat: apply schemes on editor selection"
```

Expected: commit succeeds.

## Task 5: Add Settings UI and Validation

**Files:**

- Create: `src/main/kotlin/dev/appelflap/editorschemebylanguage/settings/EditorSchemeConfigurable.kt`
- Create: `src/main/kotlin/dev/appelflap/editorschemebylanguage/settings/EditorSchemeSettingsPanel.kt`
- Create: `src/main/kotlin/dev/appelflap/editorschemebylanguage/settings/RuleTargetChooserDialog.kt`
- Test: `src/test/kotlin/dev/appelflap/editorschemebylanguage/settings/EditorSchemeSettingsPanelValidationTest.kt`

- [ ] **Step 1: Write validation tests**

Create `src/test/kotlin/dev/appelflap/editorschemebylanguage/settings/EditorSchemeSettingsPanelValidationTest.kt`:

```kotlin
package dev.appelflap.editorschemebylanguage.settings

import dev.appelflap.editorschemebylanguage.model.RuleTargetKind
import dev.appelflap.editorschemebylanguage.model.SchemeRule
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EditorSchemeSettingsPanelValidationTest {
    @Test
    fun `duplicate targets are invalid`() {
        val result = EditorSchemeSettingsValidator.validate(
            rules = listOf(
                SchemeRule(RuleTargetKind.LANGUAGE, "kotlin", "Kotlin", "Default"),
                SchemeRule(RuleTargetKind.LANGUAGE, "kotlin", "Kotlin", "Darcula"),
            ),
            installedSchemeNames = setOf("Default", "Darcula"),
        )

        assertFalse(result.valid)
    }

    @Test
    fun `missing scheme is invalid in settings`() {
        val result = EditorSchemeSettingsValidator.validate(
            rules = listOf(SchemeRule(RuleTargetKind.FILE_TYPE, "TypeScript JSX", "TypeScript JSX", "Missing")),
            installedSchemeNames = setOf("Default"),
        )

        assertFalse(result.valid)
    }

    @Test
    fun `unique targets with installed schemes are valid`() {
        val result = EditorSchemeSettingsValidator.validate(
            rules = listOf(
                SchemeRule(RuleTargetKind.LANGUAGE, "kotlin", "Kotlin", "Default"),
                SchemeRule(RuleTargetKind.FILE_TYPE, "TypeScript JSX", "TypeScript JSX", "Darcula"),
            ),
            installedSchemeNames = setOf("Default", "Darcula"),
        )

        assertTrue(result.valid)
    }
}
```

Run:

```bash
./gradlew test --tests dev.appelflap.editorschemebylanguage.settings.EditorSchemeSettingsPanelValidationTest
```

Expected: tests fail because `EditorSchemeSettingsValidator` does not exist.

- [ ] **Step 2: Add validation helper**

Create `src/main/kotlin/dev/appelflap/editorschemebylanguage/settings/EditorSchemeSettingsPanel.kt` with the validation helper first:

```kotlin
package dev.appelflap.editorschemebylanguage.settings

import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.FormBuilder
import dev.appelflap.editorschemebylanguage.EditorSchemeByLanguageBundle
import dev.appelflap.editorschemebylanguage.model.SchemeRule
import java.awt.BorderLayout
import javax.swing.DefaultCellEditor
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.table.AbstractTableModel

data class SettingsValidationResult(
    val valid: Boolean,
    val message: String? = null,
)

object EditorSchemeSettingsValidator {
    fun validate(rules: List<SchemeRule>, installedSchemeNames: Set<String>): SettingsValidationResult {
        val duplicateTargets = rules.groupingBy { it.targetKey() }.eachCount().any { it.value > 1 }
        if (duplicateTargets) {
            return SettingsValidationResult(false, EditorSchemeByLanguageBundle.message("settings.validation.duplicate"))
        }

        val missingScheme = rules.any { it.schemeName !in installedSchemeNames }
        if (missingScheme) {
            return SettingsValidationResult(false, EditorSchemeByLanguageBundle.message("settings.validation.missing.scheme"))
        }

        return SettingsValidationResult(true)
    }
}

class EditorSchemeSettingsPanel(
    installedSchemeNames: List<String>,
    private val onAddRule: (() -> SchemeRule?)? = null,
) {
    private val enabledCheckBox = JCheckBox(EditorSchemeByLanguageBundle.message("settings.enable"))
    private val tableModel = RulesTableModel(installedSchemeNames)
    private val table = JBTable(tableModel)
    private val panel: JPanel

    init {
        table.columnModel.getColumn(1).cellEditor = DefaultCellEditor(JComboBox(installedSchemeNames.toTypedArray()))

        val tablePanel = ToolbarDecorator.createDecorator(table)
            .setAddAction {
                val rule = onAddRule?.invoke() ?: return@setAddAction
                tableModel.addRule(rule)
            }
            .setRemoveAction {
                val selectedRow = table.selectedRow
                if (selectedRow >= 0) {
                    tableModel.removeRule(table.convertRowIndexToModel(selectedRow))
                }
            }
            .createPanel()

        panel = FormBuilder.createFormBuilder()
            .addComponent(enabledCheckBox)
            .addLabeledComponent(EditorSchemeByLanguageBundle.message("settings.rules.title"), tablePanel)
            .addComponentFillVertically(JPanel(BorderLayout()), 0)
            .panel
    }

    fun component(): JComponent = panel

    fun setData(enabled: Boolean, rules: List<SchemeRule>) {
        enabledCheckBox.isSelected = enabled
        tableModel.setRules(rules)
    }

    fun enabled(): Boolean = enabledCheckBox.isSelected

    fun rules(): List<SchemeRule> = tableModel.rules()

    fun validation(): SettingsValidationResult =
        EditorSchemeSettingsValidator.validate(tableModel.rules(), tableModel.installedSchemeNames.toSet())

    private class RulesTableModel(
        val installedSchemeNames: List<String>,
    ) : AbstractTableModel() {
        private val rows = mutableListOf<SchemeRule>()
        private val columns = listOf(
            EditorSchemeByLanguageBundle.message("settings.column.target"),
            EditorSchemeByLanguageBundle.message("settings.column.scheme"),
        )

        override fun getRowCount(): Int = rows.size

        override fun getColumnCount(): Int = columns.size

        override fun getColumnName(column: Int): String = columns[column]

        override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean = columnIndex == 1

        override fun getValueAt(rowIndex: Int, columnIndex: Int): Any =
            when (columnIndex) {
                0 -> "${rows[rowIndex].targetKind.name}: ${rows[rowIndex].targetDisplayName}"
                1 -> rows[rowIndex].schemeName
                else -> ""
            }

        override fun setValueAt(value: Any?, rowIndex: Int, columnIndex: Int) {
            if (columnIndex == 1 && value is String && value in installedSchemeNames) {
                rows[rowIndex].schemeName = value
                fireTableRowsUpdated(rowIndex, rowIndex)
            }
        }

        fun setRules(rules: List<SchemeRule>) {
            rows.clear()
            rows.addAll(rules.map { it.copy() })
            fireTableDataChanged()
        }

        fun addRule(rule: SchemeRule) {
            rows.add(rule)
            fireTableRowsInserted(rows.lastIndex, rows.lastIndex)
        }

        fun removeRule(index: Int) {
            rows.removeAt(index)
            fireTableRowsDeleted(index, index)
        }

        fun rules(): List<SchemeRule> = rows.map { it.copy() }
    }
}
```

- [ ] **Step 3: Add target chooser dialog**

Create `src/main/kotlin/dev/appelflap/editorschemebylanguage/settings/RuleTargetChooserDialog.kt`:

```kotlin
package dev.appelflap.editorschemebylanguage.settings

import com.intellij.lang.Language
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.SearchTextField
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import dev.appelflap.editorschemebylanguage.EditorSchemeByLanguageBundle
import dev.appelflap.editorschemebylanguage.model.RuleTargetKind
import dev.appelflap.editorschemebylanguage.model.SchemeRule
import java.awt.BorderLayout
import javax.swing.DefaultListModel
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

data class RuleTargetOption(
    val kind: RuleTargetKind,
    val id: String,
    val displayName: String,
) {
    override fun toString(): String = "${kind.name}: $displayName"
}

class RuleTargetChooserDialog : DialogWrapper(true) {
    private val allOptions = buildOptions()
    private val model = DefaultListModel<RuleTargetOption>()
    private val list = JBList(model)
    private val search = SearchTextField()

    init {
        title = EditorSchemeByLanguageBundle.message("dialog.target.title")
        search.textEditor.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) = refresh()
            override fun removeUpdate(e: DocumentEvent) = refresh()
            override fun changedUpdate(e: DocumentEvent) = refresh()
        })
        refresh()
        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout())
        panel.add(search, BorderLayout.NORTH)
        panel.add(JBScrollPane(list), BorderLayout.CENTER)
        return panel
    }

    fun selectedRule(defaultSchemeName: String): SchemeRule? {
        val selected = list.selectedValue ?: return null
        return SchemeRule(selected.kind, selected.id, selected.displayName, defaultSchemeName)
    }

    private fun refresh() {
        val query = search.text.trim().lowercase()
        model.clear()
        allOptions
            .filter { query.isEmpty() || it.displayName.lowercase().contains(query) || it.id.lowercase().contains(query) }
            .forEach(model::addElement)
        if (model.size() > 0 && list.selectedIndex < 0) {
            list.selectedIndex = 0
        }
    }

    private fun buildOptions(): List<RuleTargetOption> {
        val languageOptions = Language.getRegisteredLanguages().map {
            RuleTargetOption(RuleTargetKind.LANGUAGE, it.id, it.displayName)
        }
        val fileTypeOptions = FileTypeManager.getInstance().registeredFileTypes.map {
            RuleTargetOption(RuleTargetKind.FILE_TYPE, it.name, it.displayName)
        }
        return (languageOptions + fileTypeOptions).sortedWith(compareBy({ it.kind.name }, { it.displayName.lowercase() }))
    }
}
```

- [ ] **Step 4: Add configurable**

Create `src/main/kotlin/dev/appelflap/editorschemebylanguage/settings/EditorSchemeConfigurable.kt`:

```kotlin
package dev.appelflap.editorschemebylanguage.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.ProjectManager
import dev.appelflap.editorschemebylanguage.EditorSchemeByLanguageBundle
import dev.appelflap.editorschemebylanguage.platform.IntellijEditorSchemePlatform
import dev.appelflap.editorschemebylanguage.runtime.EditorSchemeSelectionHandler
import javax.swing.JComponent

class EditorSchemeConfigurable : Configurable {
    private var panel: EditorSchemeSettingsPanel? = null

    override fun getDisplayName(): String =
        EditorSchemeByLanguageBundle.message("settings.display.name")

    override fun createComponent(): JComponent {
        val colorsManager = EditorColorsManager.getInstance()
        val installedSchemeNames = colorsManager.allSchemes.map { it.name }.sorted()
        panel = EditorSchemeSettingsPanel(
            installedSchemeNames = installedSchemeNames,
            onAddRule = {
                val dialog = RuleTargetChooserDialog()
                if (dialog.showAndGet()) {
                    dialog.selectedRule(colorsManager.globalScheme.name)
                } else {
                    null
                }
            },
        )
        return panel!!.component()
    }

    override fun isModified(): Boolean {
        val currentPanel = panel ?: return false
        val state = EditorSchemeSettingsState.getInstance().state
        return currentPanel.enabled() != state.enabled || currentPanel.rules() != state.rules
    }

    override fun apply() {
        val currentPanel = panel ?: return
        val validation = currentPanel.validation()
        if (!validation.valid) {
            throw ConfigurationException(validation.message ?: "")
        }

        EditorSchemeSettingsState.getInstance().update(
            enabled = currentPanel.enabled(),
            rules = currentPanel.rules(),
        )
        applyToActiveEditor()
    }

    override fun reset() {
        val state = EditorSchemeSettingsState.getInstance().state
        panel?.setData(state.enabled, state.rules)
    }

    override fun disposeUIResources() {
        panel = null
    }

    private fun applyToActiveEditor() {
        val project = ProjectManager.getInstance().openProjects.firstOrNull() ?: return
        val fileEditorManager = FileEditorManager.getInstance(project)
        val editor = (fileEditorManager.selectedEditor as? TextEditor)?.editor ?: return
        val settings = EditorSchemeSettingsState.getInstance().state
        EditorSchemeSelectionHandler(
            platform = IntellijEditorSchemePlatform(project),
            enabled = { settings.enabled },
            rules = { settings.rules },
        ).applyForEditor(editor)
    }
}
```

- [ ] **Step 5: Run validation and configurable tests**

Run:

```bash
./gradlew test --tests dev.appelflap.editorschemebylanguage.settings.EditorSchemeSettingsPanelValidationTest
```

Expected: tests pass.

- [ ] **Step 6: Run full test suite**

Run:

```bash
./gradlew test
```

Expected: all tests pass.

- [ ] **Step 7: Commit settings UI**

Run:

```bash
git add src/main/kotlin/dev/appelflap/editorschemebylanguage/settings src/test/kotlin/dev/appelflap/editorschemebylanguage/settings
git commit -m "feat: add settings UI for scheme rules"
```

Expected: commit succeeds.

## Task 6: Manual IDE Verification and Polish

**Files:**

- Modify: `src/main/resources/META-INF/plugin.xml`
- Modify: `src/main/resources/messages/EditorSchemeByLanguageBundle.properties`
- Modify: source files touched by compile or IDE verification findings
- Create: `docs/manual-verification.md`

- [ ] **Step 1: Run full verification**

Run:

```bash
./gradlew test buildPlugin
```

Expected: tests pass and plugin ZIP is produced under `build/distributions`.

- [ ] **Step 2: Run the development IDE**

Run:

```bash
./gradlew runIde
```

Expected: IntelliJ IDEA development instance launches with the plugin installed.

- [ ] **Step 3: Verify settings screen**

In the development IDE:

1. Open `Settings | Tools | Editor Scheme by Language`.
2. Confirm the enable checkbox is visible.
3. Add a `Language` rule for `Kotlin`.
4. Add a `FileType` rule for `TypeScript JSX`.
5. Confirm duplicate target creation is blocked by validation.
6. Confirm scheme dropdown only accepts installed editor color schemes.

Record results in `docs/manual-verification.md`:

```markdown
# Manual Verification

## Settings

- Settings page appears under Tools: pass
- Enable checkbox appears: pass
- Language target can be selected: pass
- FileType target can be selected: pass
- Duplicate targets are rejected: pass
- Scheme selection uses installed schemes: pass

## Runtime

- Kotlin editor receives configured scheme when activated: pass
- TypeScript/TSX editor receives configured scheme when activated: pass
- Unmapped editor receives current IntelliJ default scheme: pass
- Inactive editors are not proactively scanned or updated: pass
```

- [ ] **Step 4: Verify runtime behavior**

In the development IDE:

1. Create or open a `.kt` file.
2. Create or open a `.tsx` file.
3. Configure distinct schemes for Kotlin language and TypeScript JSX file type.
4. Switch between editors.
5. Confirm only the activated editor is updated.
6. Disable the plugin in settings and apply.
7. Confirm future activations use the current default scheme.

Update `docs/manual-verification.md` with actual pass/fail notes.

- [ ] **Step 5: Fix verification findings**

For each compile, test, or manual verification failure, change only the source file responsible for that failing item, update `docs/manual-verification.md` with the result, and rerun:

```bash
./gradlew test buildPlugin
```

Expected: all tests pass and plugin ZIP builds.

- [ ] **Step 6: Commit verification polish**

Run:

```bash
git add .
git commit -m "chore: verify plugin behavior"
```

Expected: commit succeeds.

## Task 7: Final Review Readiness

**Files:**

- Modify: `README.md`
- Modify: `CHANGELOG.md`
- Modify: `docs/manual-verification.md`

- [ ] **Step 1: Update README**

Ensure `README.md` contains this user-facing summary:

```markdown
# Editor Scheme by Language

Editor Scheme by Language is an IntelliJ IDEA plugin that applies installed editor color schemes to active editors based on configured IntelliJ `Language` or `FileType` rules.

## Behavior

- Configure rules in `Settings | Tools | Editor Scheme by Language`.
- Rules map a `Language` or `FileType` to an installed editor color scheme.
- `Language` rules take precedence over `FileType` rules.
- Unmapped editors use the current IDE default from `Settings | Editor | Color Scheme`.
- The plugin updates only the active editor through the supported text-editor path.

## Development

Run tests:

```bash
./gradlew test
```

Build plugin:

```bash
./gradlew buildPlugin
```

Run development IDE:

```bash
./gradlew runIde
```
```

- [ ] **Step 2: Update changelog**

Ensure `CHANGELOG.md` contains an unreleased entry:

```markdown
# Changelog

## [0.1.0]

- Add global settings for Language/FileType to editor color scheme rules.
- Apply matching schemes to the active editor on editor selection changes.
- Fall back to the current IntelliJ default editor scheme for unmapped or missing-scheme rules.
```

- [ ] **Step 3: Run final verification**

Run:

```bash
./gradlew test buildPlugin
```

Expected: tests pass and plugin ZIP builds.

- [ ] **Step 4: Commit docs**

Run:

```bash
git add README.md CHANGELOG.md docs/manual-verification.md
git commit -m "docs: document editor scheme plugin"
```

Expected: commit succeeds.

- [ ] **Step 5: Prepare final code review**

Run:

```bash
git status --short
```

Expected: no uncommitted changes.

Run:

```bash
git log --oneline --decorate -5
```

Expected: recent commits include bootstrap, matcher, runtime listener, settings UI, verification, and docs commits.

## Self-Review

Spec coverage:

- Official plugin template bootstrap is covered by Task 1.
- IntelliJ IDEA-only conventional plugin metadata is covered by Task 1.
- Local editor-only feasibility is covered by Task 2.
- Persistent global settings are covered by Task 3 and Task 5.
- `Language > FileType > current default scheme` matching is covered by Task 3.
- Active-editor-only runtime behavior is covered by Task 4 and Task 6.
- Settings UI with enable checkbox, simple table, target chooser, scheme chooser, duplicate validation, and invalid scheme validation is covered by Task 5.
- Soft runtime fallback for missing schemes is covered by Task 3 and Task 4.
- Manual verification and final packaging are covered by Task 6 and Task 7.

Placeholder scan:

- The plan contains no `TBD`, `TODO`, or unresolved implementation placeholders.
- The only stop condition is the explicit product feasibility gate required by the spec.

Type consistency:

- `SchemeRule`, `RuleTargetKind`, `EditorSchemeContext`, `SchemeRef`, `EditorSchemeMatcher`, `EditorSchemeSettingsState`, `EditorSchemePlatform`, `IntellijEditorSchemePlatform`, and `EditorSchemeSelectionHandler` names are used consistently across tasks.
