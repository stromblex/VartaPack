package com.stromblex.vartapack.check;

import com.stromblex.vartapack.api.ModInfo;
import com.stromblex.vartapack.config.ModRule;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Reports mods that are installed but are neither required, recommended,
 * blocked, nor explicitly allowed. By default these are INFO-level so they
 * are visible to authors but do not block the player.
 */
public final class ExtraModsCheck implements Check {
    @Override public String id() { return "extraMods"; }

    @Override
    public List<CheckResult> run(CheckContext ctx) {
        Set<String> known = new HashSet<>();
        for (ModRule r : ctx.profile().requiredMods()) known.add(r.id().toLowerCase(Locale.ROOT));
        for (ModRule r : ctx.profile().recommendedMods()) known.add(r.id().toLowerCase(Locale.ROOT));
        for (ModRule r : ctx.profile().blockedMods()) known.add(r.id().toLowerCase(Locale.ROOT));
        for (String s : ctx.profile().allowedExtraMods()) {
            if (s != null) known.add(s.toLowerCase(Locale.ROOT));
        }
        // Always treat the loader, minecraft and vartapack itself as known.
        known.add("minecraft");
        known.add("java");
        known.add("vartapack");
        known.add("fabricloader");
        known.add("fabric");
        known.add("fabric-api");
        known.add("neoforge");
        known.add("forge");

        if (ctx.profile().requiredMods().isEmpty() && ctx.profile().recommendedMods().isEmpty()
                && ctx.profile().blockedMods().isEmpty() && ctx.profile().allowedExtraMods().isEmpty()) {
            // Empty profile — do not spam every installed mod as "extra".
            return List.of();
        }

        StringBuilder details = new StringBuilder();
        int count = 0;
        for (ModInfo info : ctx.installedMods().values()) {
            if (info.id() == null || info.id().isBlank()) continue;
            if (known.contains(info.id().toLowerCase(Locale.ROOT))) continue;
            if (count > 0) details.append(", ");
            details.append(info.id()).append(' ').append(info.version());
            count++;
        }
        if (count == 0) return List.of();

        Severity sev = ctx.config().extraModsSeverity();
        return List.of(new CheckResult(
                sev,
                "extra.mods",
                "Extra mods detected (" + count + ")",
                count + " mod(s) installed that are not listed in the profile.",
                details.toString(),
                sev.ordinal() >= Severity.WARNING.ordinal()));
    }
}
