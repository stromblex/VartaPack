## What it does

VartaPack validates the player's modpack installation and shows clear, actionable diagnostics.

### Validation

- **Pack Status** — CLEAN / MODIFIED / UNSUPPORTED / BROKEN
- **Required mods** — error if missing
- **Blocked mods** — error if present (detects OptiFine even without fabric.mod.json)
- **Recommended mods** — warning if missing
- **Extra mods** — info for unknown mods (allowlist supported)
- **Mod conflicts** — detects incompatible mod pairs (rules.json)
- **Java version** — minimum major version check
- **RAM** — minimum and recommended allocation check
- **Minecraft version** — validates against expected versions
- **Loader** — verifies correct mod loader
- **File integrity** — SHA-256 hash checks for critical config files (integrity.json)

### Crash Analysis

- **15 built-in patterns** — mixin failures, OOM, class errors, duplicate mods, version mismatches, etc.
- **Confidence scoring** — ranks findings by likelihood
- **Scans** crash-reports/ and logs/latest.log automatically

### Reports

- **Markdown report** — one-click copy, includes status, issues, fix instructions, environment info
- **JSON report** — machine-readable, same data
- **Privacy redaction** — strips username and home path from output

### UI

- **Issues screen** — auto-opens on critical problems, shows severity pills, issue cards with fix instructions
- **Toast notification** — startup popup with issue count
- **Continue / Block** — configurable enforcement (allowContinueAnyway, strictMode)
- **Settings screen** — in-game configuration
- **Profile wizard** — generate profile.json from current instance
- **Keybind** — V (rebindable) opens issues screen

### Doctor Mode (CLI)

Standalone diagnostics without launching Minecraft:

```bash
java -cp vartapack-*.jar com.stromblex.vartapack.doctor.DoctorCli \
  --instance /path/to/.minecraft --verbose
```

Exit codes: 0 (OK), 1 (warnings), 2 (errors).

---

## Configuration

All files go in `config/vartapack/`.

### profile.json

```json
{
  "schema": 1,
  "packId": "my-modpack",
  "packName": "My Modpack",
  "profileVersion": "1.0.0",
  "supportUrl": "https://discord.gg/example",
  "homepageUrl": "",
  "expectedMinecraftVersions": ["1.21.1"],
  "expectedLoaders": ["fabric"],
  "minimumJavaMajor": 21,
  "minimumRamMb": 4096,
  "recommendedRamMb": 6144,
  "requiredMods": [
    { "id": "sodium", "name": "Sodium", "requiredVersion": "", "reason": "" }
  ],
  "recommendedMods": [
    { "id": "modmenu", "name": "Mod Menu", "requiredVersion": "", "reason": "" }
  ],
  "blockedMods": [
    { "id": "optifine", "name": "OptiFine", "requiredVersion": "", "reason": "Incompatible with Sodium" }
  ],
  "allowedExtraMods": ["journeymap", "xaeros-minimap"]
}
```

### rules.json

Extended rules with conflict detection:

```json
{
  "schema": 1,
  "supportPolicyText": "",
  "rules": [
    {
      "id": "block-optifine",
      "type": "BLOCKED_MOD",
      "modId": "optifine",
      "displayName": "OptiFine",
      "severity": "CRITICAL",
      "category": "rendering",
      "reason": "Incompatible with Sodium.",
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
      "reason": "Both modify the rendering pipeline.",
      "fix": "Remove OptiFine.",
      "versionRangeA": "",
      "versionRangeB": ""
    }
  ]
}
```

Rule types: `REQUIRED_MOD`, `BLOCKED_MOD`, `SOFT_BLOCKED_MOD`, `RECOMMENDED_MOD`, `SUSPICIOUS_MOD`.

Mod-to-mod conflicts are configured via the separate `conflicts` array shown above
(version ranges are honored when present). The values `ALLOWED_EXTRA_MOD`,
`ENVIRONMENT_RULE`, and `FILE_RULE` are accepted for forward compatibility but
are not yet enforced; use `allowedExtraMods` in `profile.json` for extras and
`integrity.json` for file rules.

### integrity.json

File hash verification:

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
      "reason": "Pack uses optimized settings.",
      "fix": "Restore from the modpack archive."
    }
  ]
}
```

### vartapack.json

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
  "recommendedModsSeverity": "WARNING"
}
```

Severity levels: `INFO` / `WARNING` / `ERROR` / `CRITICAL`

---

## Building

```bash
./gradlew clean build
```

Output:
- `fabric/build/libs/vartapack-fabric-mc1.21-2.0.0.jar`
- `neoforge/build/libs/vartapack-neoforge-mc1.21-2.0.0.jar`

## License

MIT
