# Release Checklist

## Before releasing

- [ ] Version in `gradle.properties` matches intended release version
- [ ] CHANGELOG.md is updated with release notes
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
2. Update CHANGELOG.md.
3. Commit and push.
4. Tag: `git tag v0.1.0-beta`
5. Push tag: `git push origin v0.1.0-beta`
6. Create GitHub release from the tag.
7. Attach built JARs to the release.

## Artifact naming

Built JARs follow this pattern:

```text
vartapack-fabric-mc1.21-0.1.0-beta.jar
vartapack-neoforge-mc1.21-0.1.0-beta.jar
```

## Post-release

- [ ] Verify GitHub Actions build passed for the tag
- [ ] Download artifacts from the release and test them
- [ ] Update any external documentation links
