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
    private static final Set<String> PLATFORM_MODS = Set.of(
        "minecraft",
        "java",
        "vartapack",
        "fabricloader",
        "fabric",
        "fabric-api",
        "neoforge",
        "forge",
        "fml",
        "lowcodefml",
        "mixinextras"
    );

    private static final List<String> PLATFORM_PREFIXES = List.of(
        "fabric-",
        "neoforge-",
        "forge-"
    );

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
        known.addAll(PLATFORM_MODS);

        if (ctx.profile().requiredMods().isEmpty() && ctx.profile().recommendedMods().isEmpty()
                && ctx.profile().blockedMods().isEmpty() && ctx.profile().allowedExtraMods().isEmpty()) {
            // Empty profile — do not spam every installed mod as "extra".
            return List.of();
        }

        StringBuilder details = new StringBuilder();
        int count = 0;
        for (ModInfo info : ctx.installedMods().values()) {
            if (info.id() == null || info.id().isBlank()) continue;
            String id = info.id().toLowerCase(Locale.ROOT);
            if (known.contains(id) || isPlatformModId(id)) continue;
            if (count > 0) details.append(", ");
            details.append(info.id()).append(' ').append(info.version());
            count++;
        }
        if (count == 0) return List.of();

        Severity sev = ctx.config().extraModsSeverity();
        if (ctx.config().strictMode() && sev.ordinal() < Severity.ERROR.ordinal()) {
            sev = Severity.ERROR;
        }
        return List.of(new CheckResult(
                sev,
                "extra.mods",
                "Extra mods detected (" + count + ")",
                count + " mod(s) installed that are not listed in the profile.",
                details.toString(),
                sev.ordinal() >= Severity.WARNING.ordinal()));
    }

    public static boolean isPlatformModId(String modId) {
        if (modId == null || modId.isBlank()) return false;
        String id = modId.toLowerCase(Locale.ROOT);
        if (PLATFORM_MODS.contains(id)) return true;
        for (String prefix : PLATFORM_PREFIXES) {
            if (id.startsWith(prefix)) return true;
        }
        return false;
    }
}
