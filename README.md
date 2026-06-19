# Color Scheme per Language

[![CI](https://github.com/kpoelhekke/intellij-color-scheme-per-language/actions/workflows/ci.yml/badge.svg)](https://github.com/kpoelhekke/intellij-color-scheme-per-language/actions/workflows/ci.yml)

<!-- Plugin description -->
**Color Scheme per Language** switches the editor color scheme based on the language of the file you are working in. Map each language to one of your installed color schemes and instantly see at a glance whether you are editing Kotlin, SQL, YAML, or anything else.

- Map any language to any installed editor color scheme.
- Schemes apply automatically as you open files and switch between editor tabs.
- Diff viewers get the same per-language schemes as regular editors, and merge or preview views are covered when the IDE creates them as ordinary editors.
- Languages without a rule keep your default color scheme (`Settings | Editor | Color Scheme`).
- The editor font follows your main theme unless the per-language scheme defines its own font.
- One checkbox disables the plugin without losing your rules.
<!-- Plugin description end -->

## Installation

- **JetBrains Marketplace**: `Settings | Plugins | Marketplace`, search for **"Color Scheme per Language"**, and install.
- **Manual**: download the plugin zip from the [latest release](https://github.com/kpoelhekke/intellij-color-scheme-per-language/releases/latest), then `Settings | Plugins | ⚙ | Install Plugin from Disk…`.

## Usage

1. Open `Settings | Tools | Color Scheme per Language`.
2. Click **Add** and pick a language and the color scheme it should use.
3. Apply. Open editors update immediately.

Good to know:

- Rules match on the file's language; anything unmapped falls back to your default color scheme.
- A per-language scheme only changes colors. The editor font follows the cascade: the per-language scheme's own font if it sets one, otherwise your default color scheme's font, otherwise `Settings | Editor | Font`. So when a scheme is set to "use the default font", its editors track your main theme instead of getting stuck at a fixed size.
- Rule changes apply to open editors as soon as you hit Apply. Already-open diff, merge, and preview views are not rescanned — reopen them to pick up new rules.
- Uncheck **Enabled** in the settings page to suspend all rules; your configuration is kept.

## Compatibility

IntelliJ IDEA 2025.2 or newer. Other IntelliJ-based IDEs are not officially verified.

## Development

```bash
./gradlew check        # lint (detekt) + tests
./gradlew buildPlugin  # build the distributable zip
./gradlew runIde       # launch a sandbox IDE with the plugin
```

The section of this README between the `<!-- Plugin description -->` markers is extracted at build time and becomes the JetBrains Marketplace listing — keep it self-contained.

Commits follow [Conventional Commits](https://www.conventionalcommits.org) (enforced in CI). Releases are automated with [Release Please](https://github.com/googleapis/release-please): merging the release PR tags, publishes to the Marketplace, and updates the changelog.

## License

[Apache 2.0](LICENSE)
