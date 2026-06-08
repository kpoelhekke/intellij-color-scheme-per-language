# Diff and Merge Editor Schemes Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Apply the existing Language/FileType editor color scheme rules to normal editors, supported diff panes, merge conflict editors, and editor-backed previews using only Plugin Verifier-clean IntelliJ Platform APIs.

**Architecture:** Keep the normal `FileEditorManagerListener`, add a shared `EditorSchemeApplier`, use `DiffExtension` for diff viewers that expose `EditorDiffViewer`, and use `editorFactoryListener` for merge/previews that are created as ordinary IntelliJ editors. The rejected `MergeTool` wrapper path must be removed because Plugin Verifier flags the built-in text merge classes as internal.

**Tech Stack:** Kotlin, IntelliJ Platform SDK 2025.2.6.2, IntelliJ Platform Gradle Plugin, JUnit 4 platform tests, Plugin Verifier.

---

## Redesign Notes

- Supersedes `docs/superpowers/plans/2026-06-08-diff-merge-editor-schemes.md` after Task 1 found internal merge APIs.
- Feasibility note: `docs/diff-merge-platform-feasibility.md`
- Keep `src/main/kotlin/dev/appelflap/editorschemebylanguage/diff/DiffEditorSchemeExtension.kt`.
- Delete `src/main/kotlin/dev/appelflap/editorschemebylanguage/merge/MergeEditorSchemeTool.kt`.
- Remove `<diff.merge.MergeTool .../>` from `src/main/resources/META-INF/plugin.xml`.
- Add `<editorFactoryListener implementation="dev.appelflap.editorschemebylanguage.runtime.EditorSchemeCreationListener"/>`.

## File Structure

- Create `src/main/kotlin/dev/appelflap/editorschemebylanguage/runtime/EditorSchemeApplier.kt`: reusable rule resolution and scheme application.
- Modify `src/main/kotlin/dev/appelflap/editorschemebylanguage/runtime/EditorSchemeSelectionListener.kt`: delegate normal editor switching to `EditorSchemeApplier`.
- Create `src/main/kotlin/dev/appelflap/editorschemebylanguage/runtime/EditorSchemeCreationListener.kt`: applies schemes to newly created editors, covering merge/previews when public metadata is available.
- Modify `src/main/kotlin/dev/appelflap/editorschemebylanguage/platform/IntellijEditorSchemePlatform.kt`: add public diff-content metadata extraction.
- Create `src/main/kotlin/dev/appelflap/editorschemebylanguage/diff/DiffPaneContexts.kt`: pure context selection for side-by-side and unified diffs.
- Modify `src/main/kotlin/dev/appelflap/editorschemebylanguage/diff/DiffEditorSchemeExtension.kt`: apply schemes to diff panes using explicit content context.
- Modify `src/main/resources/META-INF/plugin.xml`: register `diff.DiffExtension` and `editorFactoryListener`, remove inert merge tool registration.
- Modify `README.md` and `docs/manual-verification.md`: document supported diff/merge/previews and the no-internal-API limitation.

### Task R1: Remove Rejected Merge Tool and Register Editor Creation Hook

**Files:**
- Create: `docs/diff-merge-platform-feasibility.md`
- Delete: `src/main/kotlin/dev/appelflap/editorschemebylanguage/merge/MergeEditorSchemeTool.kt`
- Modify: `src/main/resources/META-INF/plugin.xml`

- [ ] **Step 1: Remove the inert merge tool**

Delete `src/main/kotlin/dev/appelflap/editorschemebylanguage/merge/MergeEditorSchemeTool.kt`.

- [ ] **Step 2: Replace merge registration with editor creation registration**

In `src/main/resources/META-INF/plugin.xml`, remove:

```xml
        <diff.merge.MergeTool implementation="dev.appelflap.editorschemebylanguage.merge.MergeEditorSchemeTool"/>
```

Add this line inside the existing `<extensions defaultExtensionNs="com.intellij">` block:

```xml
        <editorFactoryListener implementation="dev.appelflap.editorschemebylanguage.runtime.EditorSchemeCreationListener"/>
```

`EditorSchemeCreationListener` is created in Task R3, so `compileKotlin` is expected to fail until that task.

- [ ] **Step 3: Confirm feasibility document exists**

Run:

```bash
test -f docs/diff-merge-platform-feasibility.md
```

Expected: command exits successfully.

- [ ] **Step 4: Commit**

```bash
git add docs/diff-merge-platform-feasibility.md src/main/resources/META-INF/plugin.xml
git rm src/main/kotlin/dev/appelflap/editorschemebylanguage/merge/MergeEditorSchemeTool.kt
git commit -m "docs: record merge API redesign"
```

### Task R2: Shared Editor Scheme Applier

**Files:**
- Create: `src/main/kotlin/dev/appelflap/editorschemebylanguage/runtime/EditorSchemeApplier.kt`
- Modify: `src/main/kotlin/dev/appelflap/editorschemebylanguage/runtime/EditorSchemeSelectionListener.kt`
- Test: `src/test/kotlin/dev/appelflap/editorschemebylanguage/runtime/EditorSchemeApplierTest.kt`

- [ ] **Step 1: Write failing applier tests**

Create `src/test/kotlin/dev/appelflap/editorschemebylanguage/runtime/EditorSchemeApplierTest.kt` using the fake-platform tests from the previous plan Task 2. The tests must cover:

```text
applyForEditor(editor, explicitContext) uses the explicit context before platform.contextFor(editor)
applyForEditor(editor) returns false and does not apply when no context exists
```

- [ ] **Step 2: Run failing test**

Run:

```bash
./gradlew test --tests dev.appelflap.editorschemebylanguage.runtime.EditorSchemeApplierTest
```

Expected: fails because `EditorSchemeApplier` does not exist.

- [ ] **Step 3: Implement `EditorSchemeApplier`**

Create `src/main/kotlin/dev/appelflap/editorschemebylanguage/runtime/EditorSchemeApplier.kt`:

```kotlin
package dev.appelflap.editorschemebylanguage.runtime

import com.intellij.openapi.editor.Editor
import dev.appelflap.editorschemebylanguage.matching.EditorSchemeMatcher
import dev.appelflap.editorschemebylanguage.model.EditorSchemeContext
import dev.appelflap.editorschemebylanguage.model.SchemeRef
import dev.appelflap.editorschemebylanguage.model.SchemeRule
import dev.appelflap.editorschemebylanguage.platform.EditorSchemePlatform
import dev.appelflap.editorschemebylanguage.platform.PlatformEditorContext

class EditorSchemeApplier(
    private val platform: EditorSchemePlatform,
    private val enabled: () -> Boolean,
    private val rules: () -> List<SchemeRule>,
) {
    fun applyForEditor(editor: Editor, context: PlatformEditorContext? = null): Boolean {
        val platformContext = context ?: platform.contextFor(editor) ?: return false
        val defaultScheme = platform.currentGlobalScheme()
        val selectedScheme = EditorSchemeMatcher.resolve(
            enabled = enabled(),
            rules = rules(),
            context = platformContext.toEditorSchemeContext(),
            installedSchemes = platform.installedSchemes().map { SchemeRef(it.name) },
            defaultScheme = SchemeRef(defaultScheme.name),
        )

        val scheme = platform.findScheme(selectedScheme.name) ?: defaultScheme
        return platform.applySchemeToEditor(editor, scheme)
    }

    private fun PlatformEditorContext.toEditorSchemeContext(): EditorSchemeContext =
        EditorSchemeContext(
            languageId = language?.id,
            languageDisplayName = language?.displayName,
            fileTypeId = fileType?.name,
            fileTypeDisplayName = fileType?.displayName,
        )
}
```

- [ ] **Step 4: Refactor selection listener to delegate**

Replace `EditorSchemeSelectionHandler` internals so it constructs an `EditorSchemeApplier` and calls `applier.applyForEditor(editor)`. Keep the public `EditorSchemeSelectionHandler.applyForEditor(editor)` method so existing tests still pass.

- [ ] **Step 5: Run tests**

Run:

```bash
./gradlew test --tests dev.appelflap.editorschemebylanguage.runtime.EditorSchemeApplierTest --tests dev.appelflap.editorschemebylanguage.runtime.EditorSchemeSelectionListenerTest
```

Expected: all selected tests pass.

- [ ] **Step 6: Commit**

```bash
git add src/main/kotlin/dev/appelflap/editorschemebylanguage/runtime/EditorSchemeApplier.kt src/main/kotlin/dev/appelflap/editorschemebylanguage/runtime/EditorSchemeSelectionListener.kt src/test/kotlin/dev/appelflap/editorschemebylanguage/runtime/EditorSchemeApplierTest.kt
git commit -m "refactor: share editor scheme application"
```

### Task R3: Editor Creation Listener for Merge and Previews

**Files:**
- Create: `src/main/kotlin/dev/appelflap/editorschemebylanguage/runtime/EditorSchemeCreationListener.kt`
- Test: `src/test/kotlin/dev/appelflap/editorschemebylanguage/runtime/EditorSchemeCreationListenerTest.kt`

- [ ] **Step 1: Write focused handler tests**

Create `src/test/kotlin/dev/appelflap/editorschemebylanguage/runtime/EditorSchemeCreationListenerTest.kt`:

```kotlin
package dev.appelflap.editorschemebylanguage.runtime

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import dev.appelflap.editorschemebylanguage.platform.EditorSchemePlatform
import dev.appelflap.editorschemebylanguage.platform.PlatformEditorContext
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

class EditorSchemeCreationListenerTest : BasePlatformTestCase() {
    fun testCreatedEditorIsAppliedWhenContextExists() {
        myFixture.configureByText("Example.kt", "fun main() = Unit")
        val manager = EditorColorsManager.getInstance()
        val platform = FakeEditorSchemePlatform(
            context = PlatformEditorContext(myFixture.file.language, myFixture.file.fileType),
            defaultScheme = manager.globalScheme,
            installedSchemes = manager.allSchemes.toList(),
        )
        val handler = EditorSchemeCreationHandler(
            applier = EditorSchemeApplier(platform, enabled = { true }, rules = { emptyList() }),
        )

        assertTrue(handler.applyForCreatedEditor(myFixture.editor))
    }

    fun testCreatedEditorIsIgnoredWhenContextIsUnavailable() {
        myFixture.configureByText("Example.kt", "fun main() = Unit")
        val manager = EditorColorsManager.getInstance()
        val platform = FakeEditorSchemePlatform(
            context = null,
            defaultScheme = manager.globalScheme,
            installedSchemes = manager.allSchemes.toList(),
        )
        val handler = EditorSchemeCreationHandler(
            applier = EditorSchemeApplier(platform, enabled = { true }, rules = { emptyList() }),
        )

        assertFalse(handler.applyForCreatedEditor(myFixture.editor))
    }

    private class FakeEditorSchemePlatform(
        private val context: PlatformEditorContext?,
        private val defaultScheme: EditorColorsScheme,
        private val installedSchemes: List<EditorColorsScheme>,
    ) : EditorSchemePlatform {
        override fun contextFor(editor: Editor): PlatformEditorContext? = context
        override fun currentGlobalScheme(): EditorColorsScheme = defaultScheme
        override fun findScheme(name: String): EditorColorsScheme? = installedSchemes.firstOrNull { it.name == name }
        override fun installedSchemes(): List<EditorColorsScheme> = installedSchemes
        override fun applySchemeToEditor(editor: Editor, scheme: EditorColorsScheme): Boolean = true
    }
}
```

- [ ] **Step 2: Run failing test**

Run:

```bash
./gradlew test --tests dev.appelflap.editorschemebylanguage.runtime.EditorSchemeCreationListenerTest
```

Expected: fails because `EditorSchemeCreationHandler` does not exist.

- [ ] **Step 3: Implement listener and handler**

Create `src/main/kotlin/dev/appelflap/editorschemebylanguage/runtime/EditorSchemeCreationListener.kt`:

```kotlin
package dev.appelflap.editorschemebylanguage.runtime

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import dev.appelflap.editorschemebylanguage.platform.IntellijEditorSchemePlatform
import dev.appelflap.editorschemebylanguage.settings.EditorSchemeSettingsState

class EditorSchemeCreationListener : EditorFactoryListener {
    override fun editorCreated(event: EditorFactoryEvent) {
        val project = event.editor.project ?: return
        val settings = EditorSchemeSettingsState.getInstance()
        val applier = EditorSchemeApplier(
            platform = IntellijEditorSchemePlatform(project),
            enabled = { settings.enabled },
            rules = { settings.rules.map { it.copy() } },
        )

        EditorSchemeCreationHandler(applier).applyForCreatedEditor(event.editor)
    }
}

class EditorSchemeCreationHandler(
    private val applier: EditorSchemeApplier,
) {
    fun applyForCreatedEditor(editor: Editor): Boolean =
        applier.applyForEditor(editor)
}
```

- [ ] **Step 4: Compile and test**

Run:

```bash
./gradlew test --tests dev.appelflap.editorschemebylanguage.runtime.EditorSchemeCreationListenerTest compileKotlin
```

Expected: tests and compilation pass. If Kotlin reports `event.editor.project` is non-nullable, simplify `val project = event.editor.project ?: return` to `val project = event.editor.project`.

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/dev/appelflap/editorschemebylanguage/runtime/EditorSchemeCreationListener.kt src/test/kotlin/dev/appelflap/editorschemebylanguage/runtime/EditorSchemeCreationListenerTest.kt src/main/resources/META-INF/plugin.xml
git commit -m "feat: apply schemes to newly created editors"
```

### Task R4: Diff Pane Contexts and Diff Extension

**Files:**
- Create: `src/main/kotlin/dev/appelflap/editorschemebylanguage/diff/DiffPaneContexts.kt`
- Modify: `src/main/kotlin/dev/appelflap/editorschemebylanguage/diff/DiffEditorSchemeExtension.kt`
- Modify: `src/main/kotlin/dev/appelflap/editorschemebylanguage/platform/IntellijEditorSchemePlatform.kt`
- Test: `src/test/kotlin/dev/appelflap/editorschemebylanguage/diff/DiffPaneContextsTest.kt`

- [ ] **Step 1: Add pure diff context tests**

Create `DiffPaneContextsTest` covering:

```text
side-by-side uses matching content index for each editor
unified prefers right/current/new content
unified falls back to left/original content
missing content returns null context
```

- [ ] **Step 2: Implement `DiffPaneContexts`**

Create `DiffPaneContexts` with `forSideBySide(contentContexts, editorCount)` and `forUnified(contentContexts, editorCount)` returning `List<PlatformEditorContext?>`.

- [ ] **Step 3: Add `IntellijEditorSchemePlatform.contextForDiffContent(content)`**

Use only public `DiffContent`, `DocumentContent`, and `FileContent` APIs:

```kotlin
val virtualFile = when (content) {
    is FileContent -> content.file
    is DocumentContent -> content.highlightFile ?: FileDocumentManager.getInstance().getFile(content.document)
    else -> null
}
val psiFile = virtualFile?.let { PsiDocumentManager.getInstance(project).findFile(it) }
val fileType = virtualFile?.fileType ?: content.contentType
```

Return `null` when both language and file type are unavailable.

- [ ] **Step 4: Implement real `DiffEditorSchemeExtension`**

For `viewer is EditorDiffViewer` and `request is ContentDiffRequest`, build an `EditorSchemeApplier`, map content contexts, and call `applier.applyForEditor(editor, explicitContext)` for each exposed editor. Unified diff (`editors.size == 1` and at least two contents) must prefer right/current/new context, then left/original.

- [ ] **Step 5: Verify**

Run:

```bash
./gradlew test --tests dev.appelflap.editorschemebylanguage.diff.DiffPaneContextsTest compileKotlin
```

Expected: tests and compilation pass.

- [ ] **Step 6: Commit**

```bash
git add src/main/kotlin/dev/appelflap/editorschemebylanguage/diff/DiffPaneContexts.kt src/main/kotlin/dev/appelflap/editorschemebylanguage/diff/DiffEditorSchemeExtension.kt src/main/kotlin/dev/appelflap/editorschemebylanguage/platform/IntellijEditorSchemePlatform.kt src/test/kotlin/dev/appelflap/editorschemebylanguage/diff/DiffPaneContextsTest.kt
git commit -m "feat: apply schemes to diff editor panes"
```

### Task R5: Documentation and Full Verification

**Files:**
- Modify: `README.md`
- Modify: `docs/manual-verification.md`

- [ ] **Step 1: Document support**

Add README wording:

```markdown
Supported diff viewers that expose editor panes through public IntelliJ Platform APIs use the same Language/FileType scheme rules as normal editors. Merge conflict views and editor-backed previews are handled when IntelliJ creates them as ordinary editor instances with public file or file type metadata. Existing diff, merge, and preview editors are not rescanned when settings change; reopen or recreate the view to apply updated mappings.
```

- [ ] **Step 2: Add manual verification notes**

Add manual steps for side-by-side diff, unified diff, merge conflict resolution, and an editor-backed preview/diff surface that comes for free through editor creation.

- [ ] **Step 3: Run final verification**

Run:

```bash
./gradlew test buildPlugin verifyPlugin
```

Expected: all tests pass, plugin zip builds, and Plugin Verifier reports all configured IDEs compatible. If Plugin Verifier flags any new API usage, stop and redesign that surface.

- [ ] **Step 4: Commit**

```bash
git add README.md docs/manual-verification.md
git commit -m "docs: document diff merge and preview support"
```
