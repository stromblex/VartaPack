# Release Checklist

## Before releasing

- [ ] Version in `gradle.properties` matches intended release version
- [ ] `artifact_suffix` in `gradle.properties` matches the intended JAR suffix (`beta`, `alpha`, or blank)
- [ ] `publish/config.json` release types and JAR paths match the intended publication channel and built file names
- [ ] CHANGELOG.md is updated with release notes
- [ ] `CHANGELOG.md` includes every public release note, even if the release was cut from another branch
- [ ] README.md examples match current config schema
- [ ] All tests pass: `./gradlew test`
- [ ] Build succeeds: `./gradlew clean build`
- [ ] Fabric JAR is present in `fabric/build/libs/`
- [ ] NeoForge JAR is present in `neoforge/build/libs/`
- [ ] Test with a clean Minecraft instance (Fabric)
- [ ] Test with a clean Minecraft instance (NeoForge)
- [ ] Verify in-game screen shows correctly
- [ ] Verify support report generates correctly
- [ ] Verify privacy redaction works
- [ ] Verify Doctor mode CLI works

## Releasing

1. Update `mod_version` in `gradle.properties`.
2. Update `artifact_suffix` in `gradle.properties` (blank omits the suffix from built JAR names).
3. Update `publish/config.json` so Modrinth/CurseForge release types and JAR paths match the publication channel and built file names.
4. Update CHANGELOG.md.
5. Commit and push.
6. Tag: `git tag v0.1.0-beta`
7. Push tag: `git push origin v0.1.0-beta`
8. Create GitHub release from the tag.
9. Attach built JARs to the release.

## Branch policy

Backport branches use the `mc/<minecraft-version>` naming scheme. They may contain earlier backport release commits when a newer Minecraft branch was created from the previous one.

Examples:

- `mc/1.21.4` can contain the `1.1.3-mc1.21.1` release commit.
- `mc/1.21.8` can contain the `1.1.3-mc1.21.4` and `1.1.3-mc1.21.1` release commits.

This is expected for chained backport branches. Do not rewrite published branch history just to hide these commits; create a fresh branch from the intended base only before publishing a new release line.

CI runs on `main`, `master`, `mc/**`, tags, pull requests, and manual dispatch. Keep `java_version` correct per branch so setup-java uses the same Java version that the branch was built for.

## Artifact naming

Built JARs follow this pattern:

```text
vartapack-fabric-mc1.21-0.1.0-beta.jar
vartapack-neoforge-mc1.21-0.1.0-beta.jar
```

When `artifact_suffix` is blank, the suffix is omitted:

```text
vartapack-fabric-mc1.21-1.1.3.jar
vartapack-neoforge-mc1.21-1.1.3.jar
```

## Post-release

- [ ] Verify GitHub Actions build passed for the tag
- [ ] Download artifacts from the release and test them
- [ ] Update any external documentation links
