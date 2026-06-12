# Changelog

## [0.3.1](https://github.com/kpoelhekke/intellij-color-scheme-per-language/compare/v0.3.0...v0.3.1) (2026-06-12)


### Bug Fixes

* replace deprecated SimpleListCellRenderer with textListCellRenderer ([6338c89](https://github.com/kpoelhekke/intellij-color-scheme-per-language/commit/6338c89a5575361dc824f650e91bc8c52b05491d))

## [0.3.0](https://github.com/kpoelhekke/intellij-color-scheme-per-language/compare/v0.2.0...v0.3.0) (2026-06-08)


### ⚠ BREAKING CHANGES

* rename the plugin to "Color Scheme per Language" (new plugin id, package, and settings storage)
* drop file-type rules; rules now target a `Language` only

### Bug Fixes

* show color scheme names in the rules table without the internal `_@user_` prefix, matching the native color scheme selector ([dfb2945](https://github.com/kpoelhekke/intellij-color-scheme-per-language/commit/dfb2945))

## [0.2.0](https://github.com/kpoelhekke/intellij-color-scheme-per-language/compare/v0.1.0...v0.2.0) (2026-06-08)


### Features

* apply matching schemes to supported diff editor panes ([bdb64c7](https://github.com/kpoelhekke/intellij-color-scheme-per-language/commit/bdb64c7))
* apply matching schemes to newly created editor-backed views, including merge and preview surfaces when IntelliJ exposes public editor metadata ([392fe50](https://github.com/kpoelhekke/intellij-color-scheme-per-language/commit/392fe50))

## 0.1.0 (2026-06-08)


### Features

* add global settings for Language/FileType to editor color scheme rules ([4ce608b](https://github.com/kpoelhekke/intellij-color-scheme-per-language/commit/4ce608b))
* apply matching schemes to the active editor on editor selection changes ([a5b423c](https://github.com/kpoelhekke/intellij-color-scheme-per-language/commit/a5b423c))
* fall back to the current IntelliJ default editor scheme for unmapped or missing-scheme rules ([55fcc5d](https://github.com/kpoelhekke/intellij-color-scheme-per-language/commit/55fcc5d))
