# Diff and Merge Editor Schemes Design

## Goal

Extend Editor Scheme by Language so supported IntelliJ diff and merge viewers apply the same configured editor color scheme rules used by normal editors.

## Scope

This is a follow-up feature to the completed editor-selection implementation.

In scope:

- Diff viewers that expose editor panes through public IntelliJ Platform APIs
- Merge conflict resolution viewers when editor panes can be reached through public APIs
- Per-pane scheme application for side-by-side diff and merge viewers
- Unified diff handling with a deterministic single-scheme fallback
- Existing global settings and rule mappings

Out of scope:

- Private or reflective access to IntelliJ diff/merge internals
- Global IDE color scheme switching
- Separate diff-specific mappings
- Live updates across already-open diff/merge viewers after settings changes
- Heuristic language detection from raw text
- User-facing notifications for unsupported diff/merge surfaces

## Behavior

Diff and merge support uses the same existing rule model:

1. `Language`
2. `FileType`
3. current IntelliJ default editor color scheme

The existing `Enable automatic editor scheme switching` checkbox controls normal editors, diff editors, and merge editors.

## Diff Viewers

For side-by-side diff viewers, each exposed editor pane should be resolved and styled independently.

Examples:

- Kotlin left pane receives the configured Kotlin scheme.
- TypeScript right pane receives the configured TypeScript or TSX scheme.
- If one side has no supported metadata, that side falls back to the current default editor scheme.

For unified diff viewers, only one editor scheme can be applied. The scheme should be chosen in this order:

1. current/new/right-side metadata
2. original/left-side metadata
3. current IntelliJ default editor scheme

## Merge Conflict Viewers

Merge conflict resolution is required for this feature, subject to a public API feasibility gate.

For three-way merge viewers:

- The result editor should use result/current file metadata.
- Side panes should use their own exposed metadata independently.
- If the merge API exposes only one reliable file context, use that context for all panes.
- Do not guess language from text content.

If merge conflict editor panes cannot be reached through public, Plugin Verifier-clean APIs, stop and redesign that part instead of using internal APIs.

## Metadata Resolution

Diff and merge panes may represent file-backed content, synthetic revisions, clipboard text, or unsaved content.

Metadata should be resolved from exposed public API data in this order:

1. backing file and PSI language, if available
2. exposed content file type, if available
3. current IntelliJ default editor scheme

The plugin must not inspect text content to infer language.

## Application Timing

On diff or merge viewer creation:

- Apply schemes once to every exposed editor pane.

On focus movement inside an existing diff or merge viewer:

- Update only the focused pane if a supported event comes through public APIs without extra lifecycle tracking.

Settings changes:

- Do not rescan or update already-open diff/merge viewers in v1.
- Newly opened or recreated diff/merge viewers use the latest settings.

## Failure Handling

Unsupported diff or merge viewer types should fail silently at runtime.

Allowed:

- Debug-level logging if a lightweight logging pattern already exists or is introduced cleanly.

Not allowed:

- User notifications for unsupported viewer types
- Runtime exceptions for unsupported surfaces
- Reflection into private viewer internals

## Architecture

Add a small diff/merge integration layer that reuses the existing runtime pieces:

- Existing `EditorSchemeSelectionHandler`
- Existing `EditorSchemePlatform`
- Existing matcher and settings state

The integration layer should:

- discover editor panes from supported diff/merge viewers
- map each pane to the best available metadata context
- call the same handler/application path used by normal editors where possible
- keep diff/merge-specific code separate from the normal `FileEditorManagerListener`

The implementation should prefer public extension points or listeners over polling or global editor scans.

## Feasibility Gates

Implementation must validate these gates early:

1. A public API path exists to observe supported diff viewer creation and access editor panes.
2. A public API path exists to observe merge conflict viewer creation and access editor panes.
3. Plugin Verifier accepts the API usage across the configured IntelliJ IDEA builds.

If any gate fails:

- Do not use internal APIs.
- Do not use reflection.
- Document the failed surface and redesign that portion.

## Testing Strategy

Use adapter-level tests first.

Tests should cover:

- side-by-side panes are resolved independently
- unified diff prefers current/new/right metadata
- merge result pane uses result/current metadata
- merge side panes use their own metadata when available
- unsupported panes are ignored
- disabled plugin prevents diff/merge scheme application

Avoid broad real UI tests in v1 unless a stable public test fixture is obvious.

Final verification must include:

- `./gradlew test`
- `./gradlew buildPlugin`
- `./gradlew verifyPlugin`

## Documentation

README wording should be precise:

> Supported diff and merge viewers that expose editor panes through public IntelliJ Platform APIs use the same Language/FileType scheme rules as normal editors.
