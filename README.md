## Features

- **RAM check** — validates minimum and recommended RAM allocation
- **Java version check** — ensures correct Java major version
- **Minecraft version check** — validates against expected versions list
- **Loader check** — verifies correct mod loader (fabric/neoforge)
- **Required mods check** — detects missing required mods (with optional version constraint)
- **Blocked mods check** — detects forbidden/incompatible mods
- **Recommended mods check** — suggests missing optional mods
- **Extra mods check** — detects mods not in the modpack's allowed list
- **Issues screen** — auto-opens on critical problems, shows severity + title + details for each issue
- **Toast notification** — clickable popup on startup when issues are found, shows issue count and keybind
- **Support report** — one-click generation, copies to clipboard, privacy-friendly (redacts paths/usernames)
- **Configurable severity levels** — each check type can be set to INFO/WARNING/ERROR/CRITICAL
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

### vartaconfig.json

Place in `config/vartapack/vartaconfig.json`:

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
| `allowContinueAnyway` | bool | `true` | Allow dismissing the issues screen |
| `includeInstalledModsInReport` | bool | `true` | Include full mod list in report |
| `includeExtraModsInReport` | bool | `true` | Include extra mods in report |
| `redactUserHomePath` | bool | `true` | Remove home path from report |
| `redactUsername` | bool | `true` | Remove OS username from report |
| `strictMode` | bool | `false` | Strict validation mode |
| `extraModsSeverity` | string | `"INFO"` | Severity for unknown mods |
| `requiredModsSeverity` | string | `"ERROR"` | Severity for missing required mods |
| `blockedModsSeverity` | string | `"ERROR"` | Severity for blocked mods |
| `recommendedModsSeverity` | string | `"WARNING"` | Severity for missing recommended mods |

Severity levels: `INFO`, `WARNING`, `ERROR`, `CRITICAL`

## Controls

- **V** — open issues screen (rebindable in controls)
- **Click toast** — open issues screen

## Building

```bash
./gradlew clean build
```

Output JARs:
- `fabric/build/libs/vartapack-fabric-*.jar`
- `neoforge/build/libs/vartapack-neoforge-*.jar`
