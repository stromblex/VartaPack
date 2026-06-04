# Changelog

## 1.1.1.0

* Added support for Minecraft 1.21.6.
* Updated Minecraft compatibility ranges and publishing metadata for the 1.21.6 release.
* Updated internal modding dependencies for current 1.21.6 releases.
* Fixed UI text colors so headings, statuses, counters, and issue details render with full opacity.
* No config changes are required.

## 1.1.0.3

* Added support for Minecraft 1.21.5.
* Updated Minecraft compatibility ranges for 1.21.5.
* Updated internal modding dependencies and Gradle tooling for current 1.21.5 releases.
* Updated publishing metadata for the 1.21.5 release.
* No config changes are required.

## 1.1.0.2

* Added support for Minecraft 1.21.4.
* Updated Minecraft compatibility ranges for 1.21.4.
* Updated internal modding dependencies for current 1.21.4 releases.
* Updated publishing metadata for the 1.21.4 release.
* No config changes are required.

## 1.1.0.1

* Added support for Minecraft 1.21.3.
* Updated Minecraft compatibility ranges for 1.21.3.
* Updated internal modding dependencies for current 1.21.3 releases.
* Updated publishing metadata for the 1.21.3 release.
* No config changes are required.

## 1.1.0

* Added support for Minecraft 1.21.2.
* Reworked in-game screens to use responsive layouts across normal, compact, and narrow windows.
* Improved scrolling, text wrapping, and fitting button labels on smaller screens.
* Updated internal modding dependencies and publishing metadata for current 1.21.2 releases.
* Existing configuration files remain compatible.

## 1.0.0.1

* Added support for Minecraft 1.21.3.
* Updated Minecraft compatibility ranges for 1.21.3.
* Updated internal modding dependencies and publishing metadata for current 1.21.3 releases.
* No config changes are required.

## 1.0.0

* Added fixed GUI scale system — VartaPack UI now renders at a stable visual scale independent of Minecraft GUI Scale setting.
* Added configurable target GUI scale with minimum readable scale clamp (UI never shrinks below 85%).
* Redesigned issues screen with two-column layout, severity counter pills, scrollable issue list, and dedicated actions panel.
* Added profile wizard screen and in-game settings screen.
* Promoted from beta to stable release. No config migration required.

## 0.1.1.2-beta

- Added support for Minecraft 1.21.2
- Fixed toast notifications — migrated to `getToastManager()` following a client API rename
- Fixed incorrect platform game version identifiers in publish configuration

## 0.1.1.1-beta

- Added support for Minecraft 1.21.1
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
