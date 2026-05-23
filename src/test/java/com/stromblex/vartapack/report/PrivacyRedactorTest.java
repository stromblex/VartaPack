package com.stromblex.vartapack.report;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PrivacyRedactorTest {
    @Test
    void redactsCommonSensitiveValues() {
        String oldHome = System.getProperty("user.home");
        String oldName = System.getProperty("user.name");
        try {
            System.setProperty("user.home", "/home/alice");
            System.setProperty("user.name", "alice");

            PrivacyRedactor redactor = new PrivacyRedactor(true, true);
            String redacted = redactor.redact("""
                    User alice at /home/alice/.minecraft
                    Email alice@example.com ip 192.168.1.10
                    token: secret-value
                    https://discord.com/api/webhooks/123/abc
                    """);

            assertTrue(redacted.contains("<user>"));
            assertTrue(redacted.contains("<user-home>"));
            assertTrue(redacted.contains("<redacted-email>"));
            assertTrue(redacted.contains("<redacted-ip>"));
            assertTrue(redacted.contains("token=<redacted>"));
            assertTrue(redacted.contains("<redacted-webhook>"));
            assertFalse(redacted.contains("alice@example.com"));
        } finally {
            restore("user.home", oldHome);
            restore("user.name", oldName);
        }
    }

    private static void restore(String key, String value) {
        if (value == null) System.clearProperty(key);
        else System.setProperty(key, value);
    }
}