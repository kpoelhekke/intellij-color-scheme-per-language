# Manual Verification

## Automated Verification

- `./gradlew test buildPlugin`: pass
- `./gradlew test buildPlugin verifyPlugin`: pass
- Plugin ZIP produced: `build/distributions/editor-scheme-by-language-0.2.0.zip`
- Plugin Verifier compatibility:
  - IU-252.28539.54: compatible
  - IU-253.33813.25: compatible
  - IU-261.25134.95: compatible
  - IU-262.7132.23: compatible

## Settings

- Settings page appears under Tools: not manually verified in GUI
- Enable checkbox appears: covered by implementation and tests, not manually verified in GUI
- Language target can be selected: covered by implementation, not manually verified in GUI
- FileType target can be selected: covered by implementation, not manually verified in GUI
- Duplicate targets are rejected: covered by `EditorSchemeSettingsPanelValidationTest`
- Scheme selection uses installed schemes: covered by implementation, not manually verified in GUI

## Runtime

- Kotlin editor receives configured scheme when activated: not manually verified in GUI
- TypeScript/TSX editor receives configured scheme when activated: not manually verified in GUI
- Unmapped editor receives current IntelliJ default scheme: covered by `EditorSchemeMatcherTest`
- Inactive editors are not proactively scanned or updated: covered by implementation shape and runtime tests

## Diff, Merge, and Previews

1. Configure at least two mappings in `Settings | Tools | Editor Scheme by Language`.
2. Open a side-by-side diff between files with different mapped languages or file types.
3. Confirm each visible editor pane uses the scheme resolved for its own side when IntelliJ exposes separate panes.
4. Open a unified diff for a mapped file.
5. Confirm the unified editor uses the current/new/right-side metadata when available, otherwise the original/left-side metadata.
6. Open IntelliJ's merge conflict resolution view for a mapped file.
7. Confirm merge editors use the configured mapping when IntelliJ creates them as ordinary editor instances with public file or file type metadata.
8. Open an editor-backed preview or diff surface that is not a main file editor.
9. Confirm it uses the same mapping when public metadata is available and otherwise falls back to the current default scheme.

## Notes

GUI verification was not completed in this non-interactive run. The plugin builds successfully, automated tests pass, and Plugin Verifier reports compatibility for all scheduled IntelliJ IDEA builds.
