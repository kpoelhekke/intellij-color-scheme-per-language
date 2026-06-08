# Diff and Merge Platform Feasibility

## Result

The initial public API probe found a verifier-clean path for diff viewers, but not for wrapping IntelliJ's built-in text merge viewer.

## Verified Public Path

- `com.intellij.diff.DiffExtension`
- `com.intellij.diff.EditorDiffViewer`
- `com.intellij.openapi.editor.event.EditorFactoryListener`

`DiffExtension` can observe supported diff viewer creation and reach exposed editor panes through `EditorDiffViewer.getEditors()`.

`EditorFactoryListener` is a public dynamic extension point (`editorFactoryListener`) and can apply the existing scheme rules to editors as they are created. This is the redesign path for merge conflict panes and other editor-backed preview surfaces that do not expose a verifier-clean dedicated viewer API.

## Failed Merge Tool Path

The planned merge probe attempted to delegate through IntelliJ's built-in text merge implementation:

- `com.intellij.diff.merge.TextMergeTool`
- `com.intellij.diff.merge.TextMergeViewer`
- `com.intellij.diff.merge.MergeThreesideViewer`

`./gradlew compileKotlin verifyPlugin` flagged that path as internal API usage. Per the design constraint, the implementation must not use those classes or reflection.

## Redesign

Do not register a custom `diff.merge.MergeTool` unless a future public API allows decorating the built-in merge viewer without internal classes.

Use `editorFactoryListener` instead:

- On `editorCreated`, read `event.editor.project`.
- Resolve metadata using the existing `IntellijEditorSchemePlatform.contextFor(editor)`.
- Apply the existing `Language > FileType > default scheme` rules through the shared runtime applier.
- If context is unavailable, fail silently.

This is O(1) per editor creation, avoids polling/global scans, and covers supported merge conflict editors when they are backed by normal IntelliJ editor instances.
