# VartaPack

A lightweight modpack integrity and support toolkit for Minecraft.

VartaPack checks whether a modpack installation is clean, modified, unsupported, or broken. It helps players understand what is wrong and gives modpack authors a clean report for support.

## Purpose

VartaPack is not a performance mod.

It is a diagnostics layer for modpacks. It validates the player environment, detects unsupported changes, explains known issues, and generates reports that are easy to paste into GitHub, Discord, CurseForge, or Modrinth support threads.

## Status model

| Status | Meaning |
| --- | --- |
| CLEAN | The instance matches the expected profile and no important issues were found. |
| MODIFIED | The player changed or added something, but no critical issue was detected. |
| UNSUPPORTED | The instance may still run, but support should be limited. |
| BROKEN | A critical issue was detected. The instance should not be considered safe or supported. |

## Features

### Validation

VartaPack can check:

- required mods
- blocked mods
- recommended mods
- extra mods
- allowed extra mods
- known mod conflicts
- Minecraft version
- mod loader
- Java version
- allocated RAM
- file integrity through SHA-256 checks
- duplicate or suspicious mod setups

### Rules

Rules can be defined in `rules.json`.

They can be used to detect blocked mods, soft-blocked mods, suspicious mods, required mods, recommended mods, and known conflicts between two mods.

Example use cases:

- block OptiFine in a Sodium-based pack
- warn about unknown rendering mods
- detect incompatible mod pairs
- show a fix instruction for each issue
- mark modified instances as limited support

### Integrity checks

VartaPack can verify selected files using SHA-256 hashes.

This is useful for:

- important config files
- tested mod jars
- pack-managed files
- baseline files that should not be changed

Integrity checks are optional. If no integrity file is provided, VartaPack works without them.

### Crash analysis

VartaPack can scan crash reports and logs for common patterns.

It can detect signs of:

- mixin failures
- missing classes
- missing dependencies
- incompatible mod sets
- duplicate mods
- wrong Java version
- out of memory errors
- renderer conflicts
- loader errors

Crash analysis does not replace manual debugging, but it gives players and authors a readable summary.

### Reports

VartaPack can generate support reports in:

- Markdown
- JSON

Reports can include:

- pack status
- environment information
- installed mods
- detected issues
- fix instructions
- crash analysis summary
- privacy redaction notice

### User interface

The in-game screen shows:

- current pack status
- issue counters
- grouped issue list
- fix instructions
- report buttons
- continue or block behavior based on config

VartaPack is designed to avoid annoying players for harmless changes.

### Doctor mode

Doctor mode can validate an instance without launching Minecraft.

Example:

```bash
java -cp vartapack-*.jar com.stromblex.vartapack.doctor.DoctorCli \
  --instance /path/to/.minecraft --verbose
```

Exit codes:

| Code | Meaning |
| --- | --- |
| 0 | No important issues found. |
| 1 | Warnings or limited-support issues found. |
| 2 | Errors or critical issues found. |

## Configuration

All configuration files are stored in:

```text
config/vartapack/
```

Common files:

```text
profile.json
rules.json
integrity.json
vartapack.json
```

## profile.json

Defines the expected modpack environment.

```json
{
  "schema": 1,
  "packId": "my-modpack",
  "packName": "My Modpack",
  "profileVersion": "1.0.0",
  "supportUrl": "https://example.com/support",
  "homepageUrl": "https://example.com",
  "expectedMinecraftVersions": ["1.21.1"],
  "expectedLoaders": ["fabric"],
  "minimumJavaMajor": 21,
  "minimumRamMb": 4096,
  "recommendedRamMb": 6144,
  "requiredMods": [
    {
      "id": "sodium",
      "name": "Sodium",
      "requiredVersion": "",
      "reason": "Required for the tested rendering stack."
    }
  ],
  "recommendedMods": [
    {
      "id": "modmenu",
      "name": "Mod Menu",
      "requiredVersion": "",
      "reason": "Recommended for easier configuration."
    }
  ],
  "blockedMods": [
    {
      "id": "optifine",
      "name": "OptiFine",
      "requiredVersion": "",
      "reason": "Known to conflict with this modpack."
    }
  ],
  "allowedExtraMods": [
    "journeymap",
    "xaeros-minimap"
  ]
}
```

## rules.json

Defines advanced validation rules and mod conflicts.

```json
{
  "schema": 1,
  "supportPolicyText": "Modified instances may receive limited support.",
  "rules": [
    {
      "id": "block-optifine",
      "type": "BLOCKED_MOD",
      "modId": "optifine",
      "displayName": "OptiFine",
      "severity": "CRITICAL",
      "category": "rendering",
      "reason": "OptiFine is not supported in this modpack.",
      "fix": "Remove OptiFine from the mods folder.",
      "versionRange": "",
      "blockContinue": true
    }
  ],
  "conflicts": [
    {
      "id": "sodium-optifine",
      "modA": "sodium",
      "modB": "optifine",
      "severity": "CRITICAL",
      "reason": "Both mods modify the rendering pipeline.",
      "fix": "Remove OptiFine and keep Sodium.",
      "versionRangeA": "",
      "versionRangeB": ""
    }
  ]
}
```

Supported rule types:

```text
REQUIRED_MOD
BLOCKED_MOD
SOFT_BLOCKED_MOD
RECOMMENDED_MOD
SUSPICIOUS_MOD
```

Mod-to-mod conflicts are configured through the `conflicts` array.

## integrity.json

Defines optional file integrity checks.

```json
{
  "schema": 1,
  "files": [
    {
      "path": "config/sodium-options.json",
      "sha256": "abc123...",
      "required": true,
      "severityIfMissing": "WARNING",
      "severityIfChanged": "INFO",
      "displayName": "Sodium Options",
      "reason": "The modpack uses tested Sodium settings.",
      "fix": "Restore this file from the original modpack archive."
    }
  ]
}
```

## vartapack.json

Controls the behavior of VartaPack.

```json
{
  "schema": 1,
  "enabled": true,
  "showToastOnStartup": true,
  "showScreenOnCriticalIssues": true,
  "allowContinueAnyway": true,
  "redactUserHomePath": true,
  "redactUsername": true,
  "strictMode": false,
  "extraModsSeverity": "INFO",
  "requiredModsSeverity": "ERROR",
  "blockedModsSeverity": "ERROR",
  "recommendedModsSeverity": "WARNING",
  "fixedGuiScale": true,
  "targetGuiScale": 2
}
```

## Severity levels

| Severity | Meaning |
| --- | --- |
| INFO | Informational message. |
| WARNING | Something may be wrong, but the instance can usually continue. |
| ERROR | A serious issue was found. |
| CRITICAL | A critical issue was found and the instance may be blocked depending on config. |

## Privacy

VartaPack does not collect telemetry.

It does not upload reports, logs, crash reports, usernames, file paths, or mod lists automatically.

Support reports are generated locally. The player decides whether to copy or share them.

Privacy redaction can remove:

- username
- home directory path
- local user path fragments

## Limitations

VartaPack cannot prevent every early startup crash.

Some crashes happen before any mod code can run. In those cases, an in-game screen cannot appear.

For these cases, use:

- Doctor mode
- crash report analysis
- static loader-level dependency or conflict rules
- clear support documentation

VartaPack should be treated as a diagnostics and support toolkit, not as a replacement for loader-level dependency management.

## Building

```bash
./gradlew clean build
```

Build outputs are generated in:

```text
fabric/build/libs/
neoforge/build/libs/
```

## Project structure

```text
common/
  shared validation, rules, integrity, crash analysis, reports

fabric/
  Fabric-specific entrypoint and integration

neoforge/
  NeoForge-specific entrypoint and integration

src/test/
  validation and report tests
```

## Recommended workflow for modpack authors

1. Create `profile.json`.
2. Add required and blocked mods.
3. Add `rules.json` for known conflicts.
4. Add `integrity.json` only for files that should be checked.
5. Test a clean instance.
6. Test a modified instance.
7. Test a broken instance.
8. Copy the generated support report and verify that it is useful.
9. Tune severities so harmless changes do not annoy players.

## License

MIT
