package com.stromblex.vartapack.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Forgiving version comparison. If both sides look semver-like, compare numerically;
 * otherwise fall back to a case-insensitive string comparison. Never throws.
 */
public final class VersionUtil {
    private static final Pattern SEMVER_PREFIX = Pattern.compile("(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?");

    private VersionUtil() {}

    public static int compare(String a, String b) {
        if (a == null) a = "";
        if (b == null) b = "";
        int[] sa = extract(a);
        int[] sb = extract(b);
        if (sa != null && sb != null) {
            for (int i = 0; i < 3; i++) {
                int c = Integer.compare(sa[i], sb[i]);
                if (c != 0) return c;
            }
            return a.compareToIgnoreCase(b);
        }
        return a.compareToIgnoreCase(b);
    }

    /**
     * Returns true if {@code installed} satisfies {@code required}.
     * For MVP this means:
     * <ul>
     *   <li>empty/blank {@code required} -> always true (handled by caller),</li>
     *   <li>required like {@code ">=1.2.3"} -> compare numerically,</li>
     *   <li>otherwise: equal (case-insensitive) or {@code installed} numerically &ge; required.</li>
     * </ul>
     */
    public static boolean satisfies(String installed, String required) {
        if (required == null || required.isBlank()) return true;
        String req = required.trim();
        try {
            if (req.startsWith(">=")) return compare(installed, req.substring(2).trim()) >= 0;
            if (req.startsWith("<=")) return compare(installed, req.substring(2).trim()) <= 0;
            if (req.startsWith(">"))  return compare(installed, req.substring(1).trim()) >  0;
            if (req.startsWith("<"))  return compare(installed, req.substring(1).trim()) <  0;
            if (req.startsWith("="))  return compare(installed, req.substring(1).trim()) == 0;
            // bare version: best-effort "installed >= required"
            if (installed != null && installed.equalsIgnoreCase(req)) return true;
            int[] si = extract(installed == null ? "" : installed);
            int[] sr = extract(req);
            if (si != null && sr != null) return compare(installed, req) >= 0;
            return installed != null && installed.equalsIgnoreCase(req);
        } catch (Throwable t) {
            return true; // never block startup over a version-string oddity
        }
    }

    private static int[] extract(String v) {
        if (v == null) return null;
        Matcher m = SEMVER_PREFIX.matcher(v);
        if (!m.find()) return null;
        int[] out = new int[3];
        out[0] = parse(m.group(1));
        out[1] = parse(m.group(2));
        out[2] = parse(m.group(3));
        return out;
    }

    private static int parse(String s) {
        if (s == null || s.isEmpty()) return 0;
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return 0; }
    }
}
