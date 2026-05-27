# Limitations

VartaPack is a diagnostics and support toolkit. It is not a crash prevention system.

## Early startup crashes

Some crashes happen before any mod code can run:

- Missing or incompatible loader dependencies
- Corrupted game files
- JVM failures
- Broken mod JARs that fail during class loading

VartaPack cannot show an in-game screen for these crashes because it has not initialized yet.

For early crashes, use:

- Doctor mode (offline validation)
- Crash report analysis (after the crash)
- Loader-level dependency declarations in your mod metadata

## Mod interactions

VartaPack cannot detect every possible mod conflict. It only detects conflicts that are explicitly defined in `rules.json`.

## Version ranges

Version range checking works for standard semver-like versions. Non-standard version strings may not match correctly.

## Integrity checks

Integrity checks use SHA-256 hashes. If a file is modified by another mod at runtime, VartaPack checks the on-disk state, not the in-memory state.

## Reports

Support reports are generated locally. They are never uploaded automatically. The player must manually copy and share them.

## Crash analysis confidence

Crash analysis uses pattern matching on log files. Findings include a confidence level. Patterns may produce false positives. VartaPack never claims certainty about crash causes.
