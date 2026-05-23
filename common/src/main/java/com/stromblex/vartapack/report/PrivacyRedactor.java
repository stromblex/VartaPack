package com.stromblex.vartapack.report;

import java.util.regex.Pattern;

/**
 * Best-effort redaction of obviously-sensitive substrings from local strings
 * before they are placed into a shareable support report.
 *
 * <p>This is deliberately conservative: VartaPack never sends data anywhere,
 * but a player who clicks "Copy Support Report" might paste the result in
 * a public channel. Redaction keeps that paste safer by default.
 */
public final class PrivacyRedactor {

    private static final Pattern EMAIL =
            Pattern.compile("(?i)[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}");
    private static final Pattern IPV4 =
            Pattern.compile("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b");
    private static final Pattern DISCORD_WEBHOOK =
            Pattern.compile("https?://(?:[a-z]+\\.)?discord(?:app)?\\.com/api/webhooks/\\S+");
    private static final Pattern TOKEN_KEYVAL =
            Pattern.compile("(?i)(token|api[_-]?key|access[_-]?token|secret|password|session[_-]?id)\\s*[:=]\\s*\\S+");

    private final boolean redactHome;
    private final boolean redactUser;
    private final String userHome;
    private final String username;

    public PrivacyRedactor(boolean redactHome, boolean redactUser) {
        this.redactHome = redactHome;
        this.redactUser = redactUser;
        this.userHome = System.getProperty("user.home", "");
        this.username = System.getProperty("user.name", "");
    }

    public String redact(String input) {
        if (input == null || input.isEmpty()) return input;
        String s = input;
        if (redactHome && !userHome.isEmpty()) {
            s = s.replace(userHome, "<user-home>");
        }
        if (redactUser && !username.isEmpty() && username.length() >= 2) {
            // Whole-word replacement to avoid butchering unrelated text.
            s = s.replaceAll("(?i)\\b" + Pattern.quote(username) + "\\b", "<user>");
        }
        s = EMAIL.matcher(s).replaceAll("<redacted-email>");
        s = DISCORD_WEBHOOK.matcher(s).replaceAll("<redacted-webhook>");
        s = IPV4.matcher(s).replaceAll("<redacted-ip>");
        s = TOKEN_KEYVAL.matcher(s).replaceAll("$1=<redacted>");
        return s;
    }
}
