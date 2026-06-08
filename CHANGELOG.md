# Changelog

## [Unreleased]

## [0.3.0]

- Rename the plugin to "Color Scheme per Language" (new plugin id, package, and settings storage).
- Drop file-type rules; rules now target a `Language` only.
- Show color scheme names in the rules table without the internal `_@user_` prefix, matching the native color scheme selector.

## [0.2.0]

- Apply matching schemes to supported diff editor panes.
- Apply matching schemes to newly created editor-backed views, including merge and preview surfaces when IntelliJ exposes public editor metadata.
- Document the verifier-clean merge support redesign.

## [0.1.0]

- Add global settings for Language/FileType to editor color scheme rules.
- Apply matching schemes to the active editor on editor selection changes.
- Fall back to the current IntelliJ default editor scheme for unmapped or missing-scheme rules.
