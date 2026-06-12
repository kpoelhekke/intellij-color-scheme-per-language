# CLAUDE.md

IntelliJ Platform plugin (Kotlin) that applies installed editor color schemes per language, driven by rules configured in `Settings | Tools | Color Scheme per Language`.

## README.md is the user documentation — keep it in sync

Whenever a feature is added, changed, or removed, update README.md **in the same change**. It is written for plugin users (and contributors only in the Development section), not as internal docs.

- The section between `<!-- Plugin description -->` and `<!-- Plugin description end -->` becomes the JetBrains Marketplace listing: build.gradle.kts extracts it at build time and `patchPluginXml` injects it into plugin.xml. Keep it self-contained, user-facing markdown; the build fails if the markers are removed.
- A behavior change that doesn't touch README.md is incomplete — either update it or state explicitly why no user-visible behavior changed.

## Commands

```bash
./gradlew check         # detekt (lint + formatting) and tests — run before committing
./gradlew test          # tests only
./gradlew buildPlugin   # distributable zip
./gradlew runIde        # sandbox IDE with the plugin
./gradlew verifyPlugin  # Marketplace compatibility verification (slow, downloads IDEs)
```

## Versioning and releases (Release Please)

- Commits follow Conventional Commits, enforced by commitlint in CI. `feat:`/`fix:` subjects become public changelog lines — write them for users.
- `feat:` bumps minor, `fix:` bumps patch; breaking changes bump minor while pre-1.0 (`bump-minor-pre-major`). 1.0 only via an explicit `Release-As:` footer.
- Releasing = merging the `chore(main): release x.y.z` PR that release-please opens; tagging, the GitHub release, and Marketplace publishing are automated in `.github/workflows/release.yml`.
- Never hand-edit: CHANGELOG.md (generated), version.txt, `.release-please-manifest.json`, or the version block in gradle.properties (between the `x-release-please` markers).
