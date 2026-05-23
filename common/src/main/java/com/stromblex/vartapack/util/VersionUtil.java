package com.stromblex.vartapack.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Forgiving version comparison. If both sides look semver-like, compare numerically;
 * otherwise fall back to a case-insensitive string comparison. Never throws.
 */
public final class VersionUtil {
    private static final Pattern NUMBER = Pattern.compile("\\d+");

    private VersionUtil() {}

    public static int compare(String a, String b) {
        ParsedVersion left = ParsedVersion.parse(a);
        ParsedVersion right = ParsedVersion.parse(b);
        return left.compareTo(right);
    }

    /**
     * Returns true if {@code installed} satisfies {@code required}.
     * Supports common mod metadata forms:
     * <ul>
     *   <li>predicate constraints like {@code >=1.2.3 <2.0.0},</li>
     *   <li>Maven/NeoForge ranges like {@code [1.2,2.0)} or {@code [1.2,)},</li>
     *   <li>caret/tilde ranges like {@code ^1.2.3} and {@code ~1.2.3},</li>
     *   <li>bare versions as best-effort minimum versions.</li>
     * </ul>
     */
    public static boolean satisfies(String installed, String required) {
        if (required == null || required.isBlank()) return true;
        String req = required.trim();
        try {
            for (String alternative : req.split("\\s*\\|\\|\\s*")) {
                if (satisfiesAll(installed, alternative.trim())) return true;
            }
            return false;
        } catch (Throwable t) {
            return true; // never block startup over a version-string oddity
        }
    }

    private static boolean satisfiesAll(String installed, String required) {
        if (required.isBlank() || required.equals("*")) return true;
        if (looksLikeRange(required)) return satisfiesAnyRange(installed, required);

        List<String> tokens = splitPredicateTokens(required);
        if (tokens.isEmpty()) return true;
        for (String token : tokens) {
            if (!satisfiesToken(installed, token)) return false;
        }
        return true;
    }

    private static boolean looksLikeRange(String required) {
        char first = required.charAt(0);
        return first == '[' || first == '(';
    }

    private static boolean satisfiesAnyRange(String installed, String required) {
        List<String> ranges = splitRanges(required);
        if (ranges.isEmpty()) return false;
        for (String range : ranges) {
            if (satisfiesRange(installed, range)) return true;
        }
        return false;
    }

    private static List<String> splitRanges(String required) {
        List<String> out = new ArrayList<>();
        int start = -1;
        for (int i = 0; i < required.length(); i++) {
            char c = required.charAt(i);
            if ((c == '[' || c == '(') && start < 0) start = i;
            if ((c == ']' || c == ')') && start >= 0) {
                out.add(required.substring(start, i + 1));
                start = -1;
            }
        }
        return out;
    }

    private static boolean satisfiesRange(String installed, String range) {
        if (range.length() < 2) return false;
        boolean includeLower = range.charAt(0) == '[';
        boolean includeUpper = range.charAt(range.length() - 1) == ']';
        String body = range.substring(1, range.length() - 1).trim();
        int comma = body.indexOf(',');
        if (comma < 0) return compare(installed, body) == 0;

        String lower = body.substring(0, comma).trim();
        String upper = body.substring(comma + 1).trim();
        if (!lower.isEmpty()) {
            int c = compare(installed, lower);
            if (includeLower ? c < 0 : c <= 0) return false;
        }
        if (!upper.isEmpty()) {
            int c = compare(installed, upper);
            if (includeUpper ? c > 0 : c >= 0) return false;
        }
        return true;
    }

    private static List<String> splitPredicateTokens(String required) {
        List<String> out = new ArrayList<>();
        for (String token : required.replace(',', ' ').split("\\s+")) {
            if (!token.isBlank()) out.add(token.trim());
        }
        return out;
    }

    private static boolean satisfiesToken(String installed, String token) {
        if (token.equals("*")) return true;
        if (token.startsWith(">=")) return compare(installed, token.substring(2).trim()) >= 0;
        if (token.startsWith("<=")) return compare(installed, token.substring(2).trim()) <= 0;
        if (token.startsWith("==")) return compare(installed, token.substring(2).trim()) == 0;
        if (token.startsWith(">")) return compare(installed, token.substring(1).trim()) > 0;
        if (token.startsWith("<")) return compare(installed, token.substring(1).trim()) < 0;
        if (token.startsWith("=")) return compare(installed, token.substring(1).trim()) == 0;
        if (token.startsWith("^")) return satisfiesCaret(installed, token.substring(1).trim());
        if (token.startsWith("~")) return satisfiesTilde(installed, token.substring(1).trim());
        if (token.endsWith(".*") || token.toLowerCase(Locale.ROOT).endsWith(".x")) {
            String prefix = token.substring(0, token.length() - 2);
            return installed != null && installed.startsWith(prefix + ".");
        }
        if (installed != null && installed.equalsIgnoreCase(token)) return true;
        return compare(installed, token) >= 0;
    }

    private static boolean satisfiesCaret(String installed, String base) {
        int[] nums = ParsedVersion.parse(base).numbers;
        String upper;
        if (nums[0] > 0) upper = (nums[0] + 1) + ".0.0";
        else if (nums[1] > 0) upper = "0." + (nums[1] + 1) + ".0";
        else upper = "0.0." + (nums[2] + 1);
        return compare(installed, base) >= 0 && compare(installed, upper) < 0;
    }

    private static boolean satisfiesTilde(String installed, String base) {
        int[] nums = ParsedVersion.parse(base).numbers;
        String upper = nums[0] + "." + (nums[1] + 1) + ".0";
        return compare(installed, base) >= 0 && compare(installed, upper) < 0;
    }

    private record ParsedVersion(String original, int[] numbers, String qualifier) implements Comparable<ParsedVersion> {
        static ParsedVersion parse(String value) {
            String raw = value == null ? "" : value.trim();
            String noBuild = raw;
            int plus = noBuild.indexOf('+');
            if (plus >= 0) noBuild = noBuild.substring(0, plus);

            String qualifier = "";
            int dash = noBuild.indexOf('-');
            if (dash >= 0) {
                qualifier = noBuild.substring(dash + 1);
                noBuild = noBuild.substring(0, dash);
            }

            int[] parts = new int[] {0, 0, 0};
            var matcher = NUMBER.matcher(noBuild);
            int i = 0;
            while (matcher.find() && i < parts.length) {
                parts[i++] = parseInt(matcher.group());
            }
            return new ParsedVersion(raw, parts, qualifier.toLowerCase(Locale.ROOT));
        }

        @Override
        public int compareTo(ParsedVersion other) {
            for (int i = 0; i < numbers.length; i++) {
                int c = Integer.compare(numbers[i], other.numbers[i]);
                if (c != 0) return c;
            }

            int q = Integer.compare(qualifierRank(qualifier), qualifierRank(other.qualifier));
            if (q != 0) return q;
            return qualifier.compareToIgnoreCase(other.qualifier);
        }

        private static int parseInt(String s) {
            try { return Integer.parseInt(s); }
            catch (NumberFormatException e) { return 0; }
        }

        private static int qualifierRank(String qualifier) {
            if (qualifier == null || qualifier.isBlank()
                    || qualifier.equals("ga") || qualifier.equals("final") || qualifier.equals("release")) {
                return 0;
            }
            String q = qualifier.split("[._-]", 2)[0];
            return switch (q) {
                case "alpha", "a" -> -5;
                case "beta", "b" -> -4;
                case "milestone", "m" -> -3;
                case "rc", "cr" -> -2;
                case "snapshot" -> -1;
                case "sp" -> 1;
                default -> -1;
            };
        }
    }
}
