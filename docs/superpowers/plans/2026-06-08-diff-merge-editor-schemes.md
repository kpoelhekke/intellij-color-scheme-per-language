# Diff and Merge Editor Schemes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Apply the existing Language/FileType editor color scheme rules to supported IntelliJ diff and merge editor panes.

**Architecture:** Add a small diff/merge integration layer that reuses the existing matcher, settings state, and editor scheme platform. Diff support uses the public `com.intellij.diff.DiffExtension` viewer-created hook and public `EditorDiffViewer.getEditors()`. Merge support uses the public `com.intellij.diff.merge.MergeTool` extension point with a delegating text merge tool around `TextMergeTool.INSTANCE`, guarded by compilation and Plugin Verifier.

**Tech Stack:** Kotlin, IntelliJ Platform SDK 2025.2.6.2, IntelliJ Platform Gradle Plugin, JUnit 4 platform tests, Plugin Verifier.

---

## Reference Context

- Spec: `docs/superpowers/specs/2026-06-08-diff-merge-editor-schemes-design.md`
- Existing normal editor listener: `src/main/kotlin/dev/appelflap/editorschemebylanguage/runtime/EditorSchemeSelectionListener.kt`
- Existing platform abstraction: `src/main/kotlin/dev/appelflap/editorschemebylanguage/platform/EditorSchemePlatform.kt`
- Existing IntelliJ platform implementation: `src/main/kotlin/dev/appelflap/editorschemebylanguage/platform/IntellijEditorSchemePlatform.kt`
- Existing plugin registration: `src/main/resources/META-INF/plugin.xml`
- Official docs consulted through Context7: IntelliJ plugin extensions are registered under `<extensions defaultExtensionNs="com.intellij">` with `implementation="..."`; IntelliJ Platform Gradle Plugin 2.x uses `intellijPlatform { bundledPlugin(...) }` for bundled plugin dependencies if needed.
- Local SDK signatures inspected from `ideaIU-2025.2.6.2-aarch64/lib/app-client.jar`:

```text
com.intellij.diff.DiffExtension.onViewerCreated(FrameDiffTool.DiffViewer, DiffContext, DiffRequest)
com.intellij.diff.EditorDiffViewer.getEditors(): List<Editor>
com.intellij.diff.requests.ContentDiffRequest.getContents(): List<DiffContent>
com.intellij.diff.contents.DiffContent.getContentType(): FileType
com.intellij.diff.contents.DocumentContent.getDocument(): Document
com.intellij.diff.contents.DocumentContent.getHighlightFile(): VirtualFile?
com.intellij.diff.contents.FileContent.getFile(): VirtualFile
com.intellij.diff.merge.MergeTool.createComponent(MergeContext, MergeRequest)
com.intellij.diff.merge.TextMergeTool.INSTANCE
com.intellij.diff.merge.TextMergeRequest.getContents(): List<DocumentContent>
com.intellij.diff.merge.TextMergeRequest.getOutputContent(): DocumentContent
com.intellij.diff.merge.TextMergeViewer.getViewer(): MergeThreesideViewer
com.intellij.diff.merge.MergeThreesideViewer.getEditor(): EditorEx
com.intellij.diff.tools.util.side.ThreesideTextDiffViewer.getEditors(): List<EditorEx>
com.intellij.diff.tools.util.side.ThreesideTextDiffViewer.getEditor(ThreeSide): EditorEx
com.intellij.diff.tools.util.side.ThreesideTextDiffViewer.getContent(ThreeSide): DocumentContent
```

## File Structure

- Modify `src/main/kotlin/dev/appelflap/editorschemebylanguage/runtime/EditorSchemeSelectionListener.kt`: keep normal editor listener but delegate selection/application logic to a reusable applier.
- Create `src/main/kotlin/dev/appelflap/editorschemebylanguage/runtime/EditorSchemeApplier.kt`: reusable scheme resolution/application for a supplied `Editor` and optional `PlatformEditorContext`.
- Create `src/main/kotlin/dev/appelflap/editorschemebylanguage/diff/DiffEditorSchemeExtension.kt`: `DiffExtension` implementation that styles public diff editor panes.
- Create `src/main/kotlin/dev/appelflap/editorschemebylanguage/diff/DiffPaneContexts.kt`: pure helpers that map diff editors and content metadata into per-pane contexts.
- Create `src/main/kotlin/dev/appelflap/editorschemebylanguage/merge/MergeEditorSchemeTool.kt`: delegating `MergeTool` for text merge viewers.
- Create `src/main/kotlin/dev/appelflap/editorschemebylanguage/merge/MergePaneContexts.kt`: pure helpers for result/current and side-pane context selection.
- Modify `src/main/resources/META-INF/plugin.xml`: register `diff.DiffExtension` and `diff.merge.MergeTool`.
- Modify `README.md`: document diff/merge support with the exact supported-surface wording from the design.
- Create tests:
  - `src/test/kotlin/dev/appelflap/editorschemebylanguage/runtime/EditorSchemeApplierTest.kt`
  - `src/test/kotlin/dev/appelflap/editorschemebylanguage/diff/DiffPaneContextsTest.kt`
  - `src/test/kotlin/dev/appelflap/editorschemebylanguage/merge/MergePaneContextsTest.kt`

### Task 1: Public API Compile Probe

**Files:**
- Create: `src/main/kotlin/dev/appelflap/editorschemebylanguage/diff/DiffEditorSchemeExtension.kt`
- Create: `src/main/kotlin/dev/appelflap/editorschemebylanguage/merge/MergeEditorSchemeTool.kt`
- Modify: `src/main/resources/META-INF/plugin.xml`

- [ ] **Step 1: Add minimal diff extension compile probe**

Create `src/main/kotlin/dev/appelflap/editorschemebylanguage/diff/DiffEditorSchemeExtension.kt`:

```kotlin
package dev.appelflap.editorschemebylanguage.diff

import com.intellij.diff.DiffContext
import com.intellij.diff.DiffExtension
import com.intellij.diff.EditorDiffViewer
import com.intellij.diff.FrameDiffTool
import com.intellij.diff.requests.DiffRequest

class DiffEditorSchemeExtension : DiffExtension() {
    override fun onViewerCreated(
        viewer: FrameDiffTool.DiffViewer,
        context: DiffContext,
        request: DiffRequest,
    ) {
        if (viewer !is EditorDiffViewer) return
        viewer.editors.forEach { it.component.repaint() }
    }
}
```

- [ ] **Step 2: Add minimal merge tool compile probe**

Create `src/main/kotlin/dev/appelflap/editorschemebylanguage/merge/MergeEditorSchemeTool.kt`:

```kotlin
package dev.appelflap.editorschemebylanguage.merge

import com.intellij.diff.merge.MergeContext
import com.intellij.diff.merge.MergeRequest
import com.intellij.diff.merge.MergeTool
import com.intellij.diff.merge.TextMergeRequest
import com.intellij.diff.merge.TextMergeTool
import com.intellij.diff.merge.TextMergeViewer

class MergeEditorSchemeTool : MergeTool {
    override fun canShow(context: MergeContext, request: MergeRequest): Boolean =
        request is TextMergeRequest && TextMergeTool.INSTANCE.canShow(context, request)

    override fun createComponent(context: MergeContext, request: MergeRequest): MergeTool.MergeViewer {
        val viewer = TextMergeTool.INSTANCE.createComponent(context, request)
        if (viewer is TextMergeViewer) {
            viewer.viewer.editor.component.repaint()
        }
        return viewer
    }
}
```

- [ ] **Step 3: Register both extension probes**

Modify `src/main/resources/META-INF/plugin.xml` inside the existing `<extensions defaultExtensionNs="com.intellij">` block:

```xml
        <diff.DiffExtension implementation="dev.appelflap.editorschemebylanguage.diff.DiffEditorSchemeExtension"/>
        <diff.merge.MergeTool implementation="dev.appelflap.editorschemebylanguage.merge.MergeEditorSchemeTool"/>
```

- [ ] **Step 4: Run compile and verifier gate**

Run:

```bash
./gradlew compileKotlin verifyPlugin
```

Expected: both tasks pass. If compilation fails because any `com.intellij.diff.*` or `com.intellij.diff.merge.*` class is unavailable, stop and write `docs/diff-merge-platform-feasibility.md` with the failing class and command output. If `verifyPlugin` reports non-public/internal/experimental API usage for these classes, stop and redesign that surface before continuing.

- [ ] **Step 5: Commit the passing probe**

```bash
git add src/main/kotlin/dev/appelflap/editorschemebylanguage/diff/DiffEditorSchemeExtension.kt src/main/kotlin/dev/appelflap/editorschemebylanguage/merge/MergeEditorSchemeTool.kt src/main/resources/META-INF/plugin.xml
git commit -m "chore: verify public diff and merge editor hooks"
```

### Task 2: Reusable Scheme Applier

**Files:**
- Create: `src/main/kotlin/dev/appelflap/editorschemebylanguage/runtime/EditorSchemeApplier.kt`
- Modify: `src/main/kotlin/dev/appelflap/editorschemebylanguage/runtime/EditorSchemeSelectionListener.kt`
- Test: `src/test/kotlin/dev/appelflap/editorschemebylanguage/runtime/EditorSchemeApplierTest.kt`

- [ ] **Step 1: Write failing tests for explicit context and missing context**

Create `src/test/kotlin/dev/appelflap/editorschemebylanguage/runtime/EditorSchemeApplierTest.kt`:

```kotlin
package dev.appelflap.editorschemebylanguage.runtime

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import dev.appelflap.editorschemebylanguage.model.RuleTargetKind
import dev.appelflap.editorschemebylanguage.model.SchemeRule
import dev.appelflap.editorschemebylanguage.platform.EditorSchemePlatform
import dev.appelflap.editorschemebylanguage.platform.PlatformEditorContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue

class EditorSchemeApplierTest : BasePlatformTestCase() {
    fun testApplyForEditorUsesExplicitContextBeforePlatformContext() {
        myFixture.configureByText("Example.kt", "fun main() = Unit")
        val manager = EditorColorsManager.getInstance()
        val defaultScheme = manager.globalScheme
        val resolvedScheme = manager.allSchemes.first { it.name != defaultScheme.name }
        val explicitContext = PlatformEditorContext(
            language = myFixture.file.language,
            fileType = myFixture.file.fileType,
        )
        val platform = FakeEditorSchemePlatform(
            context = null,
            defaultScheme = defaultScheme,
            installedSchemes = listOf(defaultScheme, resolvedScheme),
        )
        val applier = EditorSchemeApplier(
            platform = platform,
            enabled = { true },
            rules = {
                listOf(
                    SchemeRule(
                        targetKind = RuleTargetKind.LANGUAGE,
                        targetId = explicitContext.language!!.id,
                        targetDisplayName = explicitContext.language.displayName,
                        schemeName = resolvedScheme.name,
                    ),
                )
            },
        )

        assertTrue(applier.applyForEditor(myFixture.editor, explicitContext))

        assertSame(myFixture.editor, platform.appliedEditor)
        assertEquals(resolvedScheme.name, platform.appliedScheme?.name)
    }

    fun testApplyForEditorReturnsFalseWhenNoContextExists() {
        myFixture.configureByText("Example.kt", "fun main() = Unit")
        val manager = EditorColorsManager.getInstance()
        val platform = FakeEditorSchemePlatform(
            context = null,
            defaultScheme = manager.globalScheme,
            installedSchemes = manager.allSchemes.toList(),
        )
        val applier = EditorSchemeApplier(
            platform = platform,
            enabled = { true },
            rules = { emptyList() },
        )

        assertFalse(applier.applyForEditor(myFixture.editor))

        assertEquals(0, platform.applyCount)
    }

    private class FakeEditorSchemePlatform(
        private val context: PlatformEditorContext?,
        private val defaultScheme: EditorColorsScheme,
        private val installedSchemes: List<EditorColorsScheme>,
    ) : EditorSchemePlatform {
        var appliedEditor: Editor? = null
            private set
        var appliedScheme: EditorColorsScheme? = null
            private set
        var applyCount = 0
            private set

        override fun contextFor(editor: Editor): PlatformEditorContext? = context

        override fun currentGlobalScheme(): EditorColorsScheme = defaultScheme

        override fun findScheme(name: String): EditorColorsScheme? =
            installedSchemes.firstOrNull { it.name == name }

        override fun installedSchemes(): List<EditorColorsScheme> = installedSchemes

        override fun applySchemeToEditor(editor: Editor, scheme: EditorColorsScheme): Boolean {
            applyCount += 1
            appliedEditor = editor
            appliedScheme = scheme
            return true
        }
    }
}
```

- [ ] **Step 2: Run tests to verify failure**

Run:

```bash
./gradlew test --tests dev.appelflap.editorschemebylanguage.runtime.EditorSchemeApplierTest
```

Expected: fails because `EditorSchemeApplier` does not exist.

- [ ] **Step 3: Add reusable applier**

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

- [ ] **Step 4: Refactor normal editor handler to delegate**

Modify `src/main/kotlin/dev/appelflap/editorschemebylanguage/runtime/EditorSchemeSelectionListener.kt` so the file contains:

```kotlin
package dev.appelflap.editorschemebylanguage.runtime

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.fileEditor.TextEditor
import dev.appelflap.editorschemebylanguage.model.SchemeRule
import dev.appelflap.editorschemebylanguage.platform.EditorSchemePlatform
import dev.appelflap.editorschemebylanguage.platform.IntellijEditorSchemePlatform
import dev.appelflap.editorschemebylanguage.settings.EditorSchemeSettingsState

class EditorSchemeSelectionListener : FileEditorManagerListener {
    override fun selectionChanged(event: FileEditorManagerEvent) {
        val textEditor = event.newEditor as? TextEditor ?: return
        val settings = EditorSchemeSettingsState.getInstance()
        val handler = EditorSchemeSelectionHandler(
            platform = IntellijEditorSchemePlatform(event.manager.project),
            enabled = { settings.enabled },
            rules = { settings.rules.map { it.copy() } },
        )

        handler.applyForEditor(textEditor.editor)
    }
}

class EditorSchemeSelectionHandler(
    private val platform: EditorSchemePlatform,
    private val enabled: () -> Boolean,
    private val rules: () -> List<SchemeRule>,
) {
    private val applier = EditorSchemeApplier(platform, enabled, rules)

    fun applyForEditor(editor: Editor): Boolean =
        applier.applyForEditor(editor)
}
```

- [ ] **Step 5: Run runtime tests**

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

### Task 3: Diff Pane Context Mapping

**Files:**
- Create: `src/main/kotlin/dev/appelflap/editorschemebylanguage/diff/DiffPaneContexts.kt`
- Test: `src/test/kotlin/dev/appelflap/editorschemebylanguage/diff/DiffPaneContextsTest.kt`

- [ ] **Step 1: Write pure mapping tests**

Create `src/test/kotlin/dev/appelflap/editorschemebylanguage/diff/DiffPaneContextsTest.kt`:

```kotlin
package dev.appelflap.editorschemebylanguage.diff

import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull

class DiffPaneContextsTest : BasePlatformTestCase() {
    fun testSideBySideUsesMatchingContentForEachEditorIndex() {
        val fileType = FileTypeManager.getInstance().getFileTypeByExtension("kt")
        val contexts = DiffPaneContexts.forSideBySide(
            contentFileTypes = listOf(fileType, fileType),
            editorCount = 2,
        )

        assertEquals(fileType.name, contexts[0]?.fileType?.name)
        assertEquals(fileType.name, contexts[1]?.fileType?.name)
    }

    fun testUnifiedPrefersRightContentThenLeftContent() {
        val left = FileTypeManager.getInstance().getFileTypeByExtension("txt")
        val right = FileTypeManager.getInstance().getFileTypeByExtension("kt")

        val contexts = DiffPaneContexts.forUnified(
            contentFileTypes = listOf(left, right),
            editorCount = 1,
        )

        assertEquals(right.name, contexts.single()?.fileType?.name)
    }

    fun testUnifiedFallsBackToLeftContentWhenRightIsUnavailable() {
        val left = FileTypeManager.getInstance().getFileTypeByExtension("kt")

        val contexts = DiffPaneContexts.forUnified(
            contentFileTypes = listOf(left, null),
            editorCount = 1,
        )

        assertEquals(left.name, contexts.single()?.fileType?.name)
    }

    fun testUnsupportedEditorWithoutContentGetsNullContext() {
        val contexts = DiffPaneContexts.forSideBySide(
            contentFileTypes = emptyList(),
            editorCount = 1,
        )

        assertNull(contexts.single())
    }
}
```

- [ ] **Step 2: Run tests to verify failure**

Run:

```bash
./gradlew test --tests dev.appelflap.editorschemebylanguage.diff.DiffPaneContextsTest
```

Expected: fails because `DiffPaneContexts` does not exist.

- [ ] **Step 3: Add pure diff context mapper**

Create `src/main/kotlin/dev/appelflap/editorschemebylanguage/diff/DiffPaneContexts.kt`:

```kotlin
package dev.appelflap.editorschemebylanguage.diff

import com.intellij.openapi.fileTypes.FileType
import dev.appelflap.editorschemebylanguage.platform.PlatformEditorContext

object DiffPaneContexts {
    fun forSideBySide(
        contentFileTypes: List<FileType?>,
        editorCount: Int,
    ): List<PlatformEditorContext?> =
        List(editorCount) { index ->
            contentFileTypes.getOrNull(index)?.toContext()
        }

    fun forUnified(
        contentFileTypes: List<FileType?>,
        editorCount: Int,
    ): List<PlatformEditorContext?> {
        val preferred = contentFileTypes.getOrNull(1) ?: contentFileTypes.getOrNull(0)
        return List(editorCount) { preferred?.toContext() }
    }

    private fun FileType.toContext(): PlatformEditorContext =
        PlatformEditorContext(language = null, fileType = this)
}
```

- [ ] **Step 4: Run tests**

Run:

```bash
./gradlew test --tests dev.appelflap.editorschemebylanguage.diff.DiffPaneContextsTest
```

Expected: all selected tests pass.

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/dev/appelflap/editorschemebylanguage/diff/DiffPaneContexts.kt src/test/kotlin/dev/appelflap/editorschemebylanguage/diff/DiffPaneContextsTest.kt
git commit -m "feat: map diff panes to scheme contexts"
```

### Task 4: Diff Extension Implementation

**Files:**
- Modify: `src/main/kotlin/dev/appelflap/editorschemebylanguage/diff/DiffEditorSchemeExtension.kt`
- Modify: `src/main/kotlin/dev/appelflap/editorschemebylanguage/platform/IntellijEditorSchemePlatform.kt`

- [ ] **Step 1: Add a public helper for content metadata**

Modify `src/main/kotlin/dev/appelflap/editorschemebylanguage/platform/IntellijEditorSchemePlatform.kt` by adding imports:

```kotlin
import com.intellij.diff.contents.DiffContent
import com.intellij.diff.contents.DocumentContent
import com.intellij.diff.contents.FileContent
import com.intellij.openapi.fileTypes.FileType
```

Add this method inside `IntellijEditorSchemePlatform`:

```kotlin
    fun contextForDiffContent(content: DiffContent): PlatformEditorContext? {
        val virtualFile = when (content) {
            is FileContent -> content.file
            is DocumentContent -> content.highlightFile ?: FileDocumentManager.getInstance().getFile(content.document)
            else -> null
        }
        val psiFile = virtualFile?.let { PsiDocumentManager.getInstance(project).findFile(it) }
        val fileType: FileType? = virtualFile?.fileType ?: content.contentType

        return if (psiFile?.language == null && fileType == null) {
            null
        } else {
            PlatformEditorContext(
                language = psiFile?.language,
                fileType = fileType,
            )
        }
    }
```

- [ ] **Step 2: Replace the diff probe with real application logic**

Replace `src/main/kotlin/dev/appelflap/editorschemebylanguage/diff/DiffEditorSchemeExtension.kt` with:

```kotlin
package dev.appelflap.editorschemebylanguage.diff

import com.intellij.diff.DiffContext
import com.intellij.diff.DiffExtension
import com.intellij.diff.EditorDiffViewer
import com.intellij.diff.FrameDiffTool
import com.intellij.diff.requests.ContentDiffRequest
import com.intellij.diff.requests.DiffRequest
import dev.appelflap.editorschemebylanguage.platform.IntellijEditorSchemePlatform
import dev.appelflap.editorschemebylanguage.runtime.EditorSchemeApplier
import dev.appelflap.editorschemebylanguage.settings.EditorSchemeSettingsState

class DiffEditorSchemeExtension : DiffExtension() {
    override fun onViewerCreated(
        viewer: FrameDiffTool.DiffViewer,
        context: DiffContext,
        request: DiffRequest,
    ) {
        val project = context.project ?: return
        if (viewer !is EditorDiffViewer) return
        val contentRequest = request as? ContentDiffRequest ?: return

        val platform = IntellijEditorSchemePlatform(project)
        val settings = EditorSchemeSettingsState.getInstance()
        val applier = EditorSchemeApplier(
            platform = platform,
            enabled = { settings.enabled },
            rules = { settings.rules.map { it.copy() } },
        )
        val editors = viewer.editors
        val contentContexts = contentRequest.contents.map { platform.contextForDiffContent(it) }
        val contexts = if (editors.size == 1 && contentContexts.size >= 2) {
            DiffPaneContexts.forUnified(
                contentFileTypes = contentContexts.map { it?.fileType },
                editorCount = editors.size,
            ).mapIndexed { index, fallback -> contentContexts.getOrNull(1) ?: contentContexts.getOrNull(0) ?: fallback }
        } else {
            DiffPaneContexts.forSideBySide(
                contentFileTypes = contentContexts.map { it?.fileType },
                editorCount = editors.size,
            ).mapIndexed { index, fallback -> contentContexts.getOrNull(index) ?: fallback }
        }

        editors.forEachIndexed { index, editor ->
            applier.applyForEditor(editor, contexts.getOrNull(index))
        }
    }
}
```

- [ ] **Step 3: Compile**

Run:

```bash
./gradlew compileKotlin
```

Expected: compilation passes. If Kotlin reports that `context.project` is non-nullable, simplify `val project = context.project ?: return` to `val project = context.project` and rerun.

- [ ] **Step 4: Run focused tests**

Run:

```bash
./gradlew test --tests dev.appelflap.editorschemebylanguage.diff.DiffPaneContextsTest --tests dev.appelflap.editorschemebylanguage.runtime.EditorSchemeApplierTest
```

Expected: all selected tests pass.

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/dev/appelflap/editorschemebylanguage/diff/DiffEditorSchemeExtension.kt src/main/kotlin/dev/appelflap/editorschemebylanguage/platform/IntellijEditorSchemePlatform.kt
git commit -m "feat: apply schemes to diff editor panes"
```

### Task 5: Merge Pane Context Mapping and Tool

**Files:**
- Create: `src/main/kotlin/dev/appelflap/editorschemebylanguage/merge/MergePaneContexts.kt`
- Modify: `src/main/kotlin/dev/appelflap/editorschemebylanguage/merge/MergeEditorSchemeTool.kt`
- Test: `src/test/kotlin/dev/appelflap/editorschemebylanguage/merge/MergePaneContextsTest.kt`

- [ ] **Step 1: Write pure merge context tests**

Create `src/test/kotlin/dev/appelflap/editorschemebylanguage/merge/MergePaneContextsTest.kt`:

```kotlin
package dev.appelflap.editorschemebylanguage.merge

import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import dev.appelflap.editorschemebylanguage.platform.PlatformEditorContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull

class MergePaneContextsTest : BasePlatformTestCase() {
    fun testResultContextUsesOutputContent() {
        val fileType = FileTypeManager.getInstance().getFileTypeByExtension("kt")
        val output = PlatformEditorContext(language = null, fileType = fileType)

        assertEquals(output, MergePaneContexts.forResultEditor(output, emptyList()))
    }

    fun testResultContextFallsBackToSingleReliableSideContext() {
        val fileType = FileTypeManager.getInstance().getFileTypeByExtension("kt")
        val side = PlatformEditorContext(language = null, fileType = fileType)

        assertEquals(side, MergePaneContexts.forResultEditor(null, listOf(side)))
    }

    fun testResultContextIsNullWithoutMetadata() {
        assertNull(MergePaneContexts.forResultEditor(null, emptyList()))
    }

    fun testSideEditorsUseTheirOwnContexts() {
        val left = PlatformEditorContext(
            language = null,
            fileType = FileTypeManager.getInstance().getFileTypeByExtension("txt"),
        )
        val base = PlatformEditorContext(
            language = null,
            fileType = FileTypeManager.getInstance().getFileTypeByExtension("java"),
        )
        val right = PlatformEditorContext(
            language = null,
            fileType = FileTypeManager.getInstance().getFileTypeByExtension("kt"),
        )

        val contexts = MergePaneContexts.forThreeSideEditors(
            sideContexts = listOf(left, base, right),
            editorCount = 3,
            fallbackContext = null,
        )

        assertEquals(left, contexts[0])
        assertEquals(base, contexts[1])
        assertEquals(right, contexts[2])
    }

    fun testSideEditorsUseSingleReliableFallbackContext() {
        val fallback = PlatformEditorContext(
            language = null,
            fileType = FileTypeManager.getInstance().getFileTypeByExtension("kt"),
        )

        val contexts = MergePaneContexts.forThreeSideEditors(
            sideContexts = emptyList(),
            editorCount = 3,
            fallbackContext = fallback,
        )

        assertEquals(listOf(fallback, fallback, fallback), contexts)
    }
}
```

- [ ] **Step 2: Run tests to verify failure**

Run:

```bash
./gradlew test --tests dev.appelflap.editorschemebylanguage.merge.MergePaneContextsTest
```

Expected: fails because `MergePaneContexts` does not exist.

- [ ] **Step 3: Add merge context helper**

Create `src/main/kotlin/dev/appelflap/editorschemebylanguage/merge/MergePaneContexts.kt`:

```kotlin
package dev.appelflap.editorschemebylanguage.merge

import dev.appelflap.editorschemebylanguage.platform.PlatformEditorContext

object MergePaneContexts {
    fun forResultEditor(
        outputContext: PlatformEditorContext?,
        sideContexts: List<PlatformEditorContext?>,
    ): PlatformEditorContext? =
        outputContext ?: sideContexts.filterNotNull().distinct().singleOrNull()

    fun forThreeSideEditors(
        sideContexts: List<PlatformEditorContext?>,
        editorCount: Int,
        fallbackContext: PlatformEditorContext?,
    ): List<PlatformEditorContext?> =
        List(editorCount) { index ->
            sideContexts.getOrNull(index) ?: fallbackContext
        }
}
```

- [ ] **Step 4: Replace the merge probe with real application logic**

Replace `src/main/kotlin/dev/appelflap/editorschemebylanguage/merge/MergeEditorSchemeTool.kt` with:

```kotlin
package dev.appelflap.editorschemebylanguage.merge

import com.intellij.diff.merge.MergeContext
import com.intellij.diff.merge.MergeRequest
import com.intellij.diff.merge.MergeTool
import com.intellij.diff.merge.TextMergeRequest
import com.intellij.diff.merge.TextMergeTool
import com.intellij.diff.merge.TextMergeViewer
import com.intellij.diff.util.ThreeSide
import dev.appelflap.editorschemebylanguage.platform.IntellijEditorSchemePlatform
import dev.appelflap.editorschemebylanguage.runtime.EditorSchemeApplier
import dev.appelflap.editorschemebylanguage.settings.EditorSchemeSettingsState

class MergeEditorSchemeTool : MergeTool {
    override fun canShow(context: MergeContext, request: MergeRequest): Boolean =
        request is TextMergeRequest && TextMergeTool.INSTANCE.canShow(context, request)

    override fun createComponent(context: MergeContext, request: MergeRequest): MergeTool.MergeViewer {
        val viewer = TextMergeTool.INSTANCE.createComponent(context, request)
        val textRequest = request as? TextMergeRequest ?: return viewer
        val textViewer = viewer as? TextMergeViewer ?: return viewer

        val project = context.project ?: return viewer
        val platform = IntellijEditorSchemePlatform(project)
        val settings = EditorSchemeSettingsState.getInstance()
        val applier = EditorSchemeApplier(
            platform = platform,
            enabled = { settings.enabled },
            rules = { settings.rules.map { it.copy() } },
        )

        val outputContext = platform.contextForDiffContent(textRequest.outputContent)
        val sideContexts = textRequest.contents.map { platform.contextForDiffContent(it) }
        val resultContext = MergePaneContexts.forResultEditor(outputContext, sideContexts)
        applier.applyForEditor(textViewer.viewer.editor, resultContext)
        val sideViewer = textViewer.viewer
        val sideEditorContexts = MergePaneContexts.forThreeSideEditors(
            sideContexts = ThreeSide.entries.map { side ->
                platform.contextForDiffContent(sideViewer.getContent(side))
            },
            editorCount = sideViewer.editors.size,
            fallbackContext = resultContext,
        )
        sideViewer.editors.forEachIndexed { index, editor ->
            applier.applyForEditor(editor, sideEditorContexts.getOrNull(index))
        }

        return viewer
    }
}
```

- [ ] **Step 5: Run tests and compile**

Run:

```bash
./gradlew test --tests dev.appelflap.editorschemebylanguage.merge.MergePaneContextsTest compileKotlin
```

Expected: tests and compilation pass. If Kotlin reports that `context.project` is non-nullable, simplify `val project = context.project ?: return viewer` to `val project = context.project` and rerun. If Kotlin reports that `ThreeSide.entries` is unavailable, replace it with `ThreeSide.values().toList()` and rerun.

- [ ] **Step 6: Commit**

```bash
git add src/main/kotlin/dev/appelflap/editorschemebylanguage/merge/MergePaneContexts.kt src/main/kotlin/dev/appelflap/editorschemebylanguage/merge/MergeEditorSchemeTool.kt src/test/kotlin/dev/appelflap/editorschemebylanguage/merge/MergePaneContextsTest.kt
git commit -m "feat: apply schemes to merge editor panes"
```

### Task 6: Documentation and Full Verification

**Files:**
- Modify: `README.md`
- Modify: `docs/manual-verification.md`

- [ ] **Step 1: Update README support wording**

Add this paragraph to `README.md` after the existing feature description:

```markdown
Supported diff and merge viewers that expose editor panes through public IntelliJ Platform APIs use the same Language/FileType scheme rules as normal editors. Existing diff and merge viewers are not rescanned when settings change; reopen or recreate the viewer to apply updated mappings.
```

- [ ] **Step 2: Update manual verification notes**

Add this section to `docs/manual-verification.md`:

```markdown
## Diff and Merge Views

1. Configure at least two mappings in Settings | Tools | Editor Scheme by Language.
2. Open a side-by-side diff between files with different mapped languages or file types.
3. Confirm each visible editor pane uses the scheme resolved for its own side when IntelliJ exposes separate panes.
4. Open a unified diff for a mapped file.
5. Confirm the unified editor uses the current/new/right-side metadata when available, otherwise the original/left-side metadata.
6. Open IntelliJ's merge conflict resolution view for a mapped file.
7. Confirm the result editor uses the result/current file mapping, or the single reliable file context when only one is exposed.
```

- [ ] **Step 3: Run all tests**

Run:

```bash
./gradlew test
```

Expected: all tests pass.

- [ ] **Step 4: Build plugin**

Run:

```bash
./gradlew buildPlugin
```

Expected: plugin zip builds successfully under `build/distributions/`.

- [ ] **Step 5: Run Plugin Verifier**

Run:

```bash
./gradlew verifyPlugin
```

Expected: Plugin Verifier reports no compatibility problems for configured IDEs. If it flags `DiffExtension`, `MergeTool`, `TextMergeTool`, `TextMergeViewer`, or `MergeThreesideViewer` as internal/non-public/non-extendable in any target IDE, revert the affected implementation commit only, document the incompatible surface in `docs/diff-merge-platform-feasibility.md`, and stop for redesign.

- [ ] **Step 6: Commit**

```bash
git add README.md docs/manual-verification.md
git commit -m "docs: document diff and merge scheme support"
```

## Self-Review

- Spec coverage: normal editor rules are reused through `EditorSchemeApplier`; diff viewer creation is covered through `DiffExtension`; side-by-side and unified fallback behavior are covered by `DiffPaneContexts`; merge conflict result and side editor support are covered through the delegating `MergeTool`; unsupported surfaces are ignored by type checks; existing settings remain the only user-facing configuration.
- Known limitation: merge side-pane support depends on Plugin Verifier accepting the public `ThreesideTextDiffViewer` methods inherited by `MergeThreesideViewer`. If any target IDE flags that usage as non-public or incompatible, revert the merge side-pane portion and redesign instead of using reflection.
- Placeholder scan: no `TBD`, `TODO`, or unspecified implementation steps remain.
- Type consistency: `EditorSchemeApplier`, `DiffPaneContexts`, `MergePaneContexts`, `DiffEditorSchemeExtension`, and `MergeEditorSchemeTool` names are used consistently across tasks.
