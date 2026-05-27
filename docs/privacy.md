# Privacy

VartaPack is designed to be privacy-friendly.

## No telemetry

VartaPack does not collect, transmit, or store any telemetry data.

## No network requests

VartaPack makes no HTTP requests, no DNS lookups, and no socket connections. All processing is local.

## No automatic uploads

Support reports are generated locally and stored only in the clipboard when the player explicitly copies them. Nothing is sent anywhere automatically.

## Redaction

When generating support reports, VartaPack redacts:

- The player's OS username
- The player's home directory path
- Email addresses
- IPv4 addresses
- Discord webhook URLs
- API keys and tokens in key-value patterns

Redaction is enabled by default and can be configured in `vartapack.json`:

```json
{
  "redactUserHomePath": true,
  "redactUsername": true
}
```

## Installed mods list

The support report can include a list of installed mods. This is controlled by:

```json
{
  "includeInstalledModsInReport": true,
  "includeExtraModsInReport": true
}
```

Set these to `false` if your users prefer not to share their mod list.

## Player control

The player always decides whether to copy or share a report. VartaPack only generates the text.
