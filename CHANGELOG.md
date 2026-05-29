# Changelog

## 0.1.1.2-beta

- Fixed toast notifications — migrated to `getToastManager()` following a client API rename
- Fixed incorrect platform game version identifiers in publish configuration

## 0.1.1.1-beta

- Updated all dependency versions to latest stable releases
- Simplified version numbering in mod distribution (removed loader-name suffix)
- Upload display name now derived from JAR filename

## 0.1.1-beta

- Fixed: blocking-notice text no longer overlaps buttons on narrow screens


## 0.1.0-beta

Initial beta release

- Pack status model: CLEAN, MODIFIED, UNSUPPORTED, BROKEN
- Validation engine: required, blocked, recommended, extra mods, MC version, loader, Java, RAM
- Rules system with BLOCKED_MOD, SOFT_BLOCKED_MOD, REQUIRED_MOD, RECOMMENDED_MOD, SUSPICIOUS_MOD, and conflict detection
- SHA-256 file integrity verification with path-traversal protection
- Crash log and crash report analysis with 16 pattern detectors
- Markdown and JSON support report generation with privacy redaction
- In-game issues screen, startup toast, and configurable severity levels
- Doctor mode CLI for offline instance validation
- Profile wizard for easy in-game setup
- No profile.json created on first run — modpack authors add their own
