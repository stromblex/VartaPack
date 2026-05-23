## Features

- **RAM check** — validates minimum and recommended RAM allocation
- **Java version check** — ensures correct Java major version
- **Minecraft version check** — validates against expected versions list
- **Loader check** — verifies correct mod loader (fabric/neoforge)
- **Required mods check** — detects missing required mods (with optional version constraint)
- **Blocked mods check** — detects forbidden/incompatible mods
- **Recommended mods check** — suggests missing optional mods
- **Extra mods check** — detects mods not in the modpack's allowed list
- **Issues screen** — auto-opens on ERROR/CRITICAL problems, shows a compact severity summary and issue details
- **Toast notification** — clickable popup on startup when issues are found, shows issue count and keybind
- **Support report** — one-click generation, copies to clipboard, privacy-friendly (redacts paths/usernames)
- **Configurable severity levels** — each check type can be set to INFO/WARNING/ERROR/CRITICAL
- **In-game settings** — configure startup checks, report privacy, severity levels, and strict mode from the issues screen
- **Profile wizard** — generate a baseline `profile.json` from the currently running instance
- **Strict client gate** — when `strictMode` is enabled or `allowContinueAnyway` is disabled, ERROR/CRITICAL issues block closing the issues screen
- **Privacy redaction** — removes OS username and home directory path from reports
- **Keybind** — rebindable key to open issues screen (default: V), works on title screen and in-game
- **Multi-loader** — single codebase for Fabric and NeoForge

## For Modpack Authors

### profile.json

Place in `config/vartapack/profile.json`:

```json
{
  "schema": 1,
  "packId": "my-modpack",
  "packName": "My Modpack",
  "profileVersion": "1.0.0",
  "supportUrl": "https://discord.gg/example",
  "homepageUrl": "https://example.com",
  "expectedMinecraftVersions": ["1.21.1"],
  "expectedLoaders": ["fabric"],
  "minimumJavaMajor": 21,
  "minimumRamMb": 4096,
  "recommendedRamMb": 6144,
  "requiredMods": [
    { "id": "sodium", "name": "Sodium", "requiredVersion": "", "reason": "" }
  ],
  "blockedMods": [
    { "id": "optifine", "name": "OptiFine", "requiredVersion": "", "reason": "Incompatible with Sodium" }
  ],
  "recommendedMods": [
    { "id": "modmenu", "name": "Mod Menu", "requiredVersion": "", "reason": "" }
  ],
  "allowedExtraMods": ["fabric-api", "cloth-config"]
}
```

| Field | Type | Description |
|-------|------|-------------|
| `schema` | int | Config schema version (always `1`) |
| `packId` | string | Unique modpack identifier |
| `packName` | string | Display name |
| `profileVersion` | string | Profile version for tracking |
| `supportUrl` | string | Link shown in support report |
| `homepageUrl` | string | Modpack homepage link |
| `expectedMinecraftVersions` | string[] | Allowed MC versions |
| `expectedLoaders` | string[] | Allowed loaders (`fabric`, `neoforge`) |
| `minimumJavaMajor` | int | Minimum Java version (e.g. `21`) |
| `minimumRamMb` | int | Minimum RAM in MB (below = ERROR) |
| `recommendedRamMb` | int | Recommended RAM in MB (below = WARNING) |
| `requiredMods` | ModRule[] | Mods that must be installed |
| `blockedMods` | ModRule[] | Mods that must NOT be installed |
| `recommendedMods` | ModRule[] | Optional but suggested mods |
| `allowedExtraMods` | string[] | Mod IDs that won't trigger "extra mod" warning |

**ModRule fields:** `id` (mod ID), `name` (display name), `requiredVersion` (version constraint, optional), `reason` (explanation text, optional)

Supported `requiredVersion` forms include simple minimum versions (`"1.5.0"`), predicates (`">=1.2 <2.0"`), Maven/NeoForge-style ranges (`"[1.2,2.0)"`, `"[1.2,)"`), and caret/tilde ranges (`"^1.2.3"`, `"~1.2.3"`).

### vartapack.json

Place in `config/vartapack/vartapack.json`:

> Legacy note: older docs mentioned `vartaconfig.json`. If that file exists and `vartapack.json` does not, VartaPack migrates it automatically.

```json
{
  "schema": 1,
  "enabled": true,
  "showToastOnStartup": true,
  "showScreenOnCriticalIssues": true,
  "allowContinueAnyway": true,
  "includeInstalledModsInReport": true,
  "includeExtraModsInReport": true,
  "redactUserHomePath": true,
  "redactUsername": true,
  "strictMode": false,
  "extraModsSeverity": "INFO",
  "requiredModsSeverity": "ERROR",
  "blockedModsSeverity": "ERROR",
  "recommendedModsSeverity": "WARNING"
}
```

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `enabled` | bool | `true` | Enable/disable all checks |
| `showToastOnStartup` | bool | `true` | Show toast notification when issues found |
| `showScreenOnCriticalIssues` | bool | `true` | Auto-open screen on ERROR/CRITICAL |
| `allowContinueAnyway` | bool | `true` | Allow dismissing the issues screen when ERROR/CRITICAL issues exist |
| `includeInstalledModsInReport` | bool | `true` | Include full mod list in report |
| `includeExtraModsInReport` | bool | `true` | Include extra mods in report |
| `redactUserHomePath` | bool | `true` | Remove home path from report |
| `redactUsername` | bool | `true` | Remove OS username from report |
| `strictMode` | bool | `false` | Treat extra mods as ERROR and block continue until ERROR/CRITICAL issues are fixed |
| `extraModsSeverity` | string | `"INFO"` | Severity for unknown mods |
| `requiredModsSeverity` | string | `"ERROR"` | Severity for missing required mods |
| `blockedModsSeverity` | string | `"ERROR"` | Severity for blocked mods |
| `recommendedModsSeverity` | string | `"WARNING"` | Severity for missing recommended mods |

Severity levels: `INFO`, `WARNING`, `ERROR`, `CRITICAL`

## Controls

- **V** — open issues screen (rebindable in controls)
- **Click toast** — open issues screen
- **Settings** — open the in-game VartaPack settings screen
- **Profile Wizard** — create/update a baseline `profile.json` from the current instance

## Enforcement Notes

VartaPack currently provides a strict client-side gate. It can block closing the issues screen when the local profile has ERROR/CRITICAL issues, but it does not yet implement a server handshake that rejects clients during multiplayer login. If you need server-authoritative pack matching, pair VartaPack with a dedicated compatibility/checker mod or add a server-side handshake layer on top of the same `packId`/`profileVersion` profile data.

## Building

```bash
./gradlew clean build
```

Output JARs:
- `fabric/build/libs/vartapack-fabric-*.jar`
- `neoforge/build/libs/vartapack-neoforge-*.jar`
