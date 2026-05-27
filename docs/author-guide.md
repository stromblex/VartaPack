# Author Guide

This guide explains how to integrate VartaPack into your modpack.

## Setup

1. Add the VartaPack jar to your modpack's `mods/` folder.
2. Create the config directory: `config/vartapack/`.
3. Add a `profile.json` that describes your pack.
4. Optionally add `rules.json` for advanced validation rules.
5. Optionally add `integrity.json` for file hash verification.
6. Optionally add `vartapack.json` to override default behavior.

## Minimum configuration

A `profile.json` is the only required file. Without it, VartaPack uses safe defaults and does nothing harmful.

## Recommended workflow

1. Start with a minimal `profile.json` containing your pack ID, name, and expected Minecraft version.
2. Add required mods that your pack depends on.
3. Add blocked mods that are known to conflict.
4. Test with a clean instance and verify status is CLEAN.
5. Test with an extra mod added and verify status is MODIFIED.
6. Test with a required mod removed and verify status is BROKEN.
7. Generate a support report and verify it is useful and redacted.
8. Add `rules.json` only if you need advanced conflict detection.
9. Add `integrity.json` only for config files or assets that must not change.

## Severity tuning

The default severities are:

| Check | Default severity |
| --- | --- |
| Extra mods | INFO |
| Required mods missing | ERROR |
| Blocked mods present | ERROR |
| Recommended mods missing | WARNING |

Override these in `vartapack.json` if your pack has different needs.

## Strict mode

When `strictMode` is true in `vartapack.json`:

- Extra mods are elevated to ERROR severity.
- The issues screen blocks continue when errors are present.

Use strict mode only if your pack has a strict no-modification policy.

## Doctor mode

Doctor mode validates an instance without launching Minecraft.

```bash
java -cp vartapack-*.jar com.stromblex.vartapack.doctor.DoctorCli \
  --instance /path/to/.minecraft
```

Use `--json` for machine-readable output. Use `--verbose` for detailed logging.

Exit codes: 0 = clean, 1 = warnings, 2 = errors.
