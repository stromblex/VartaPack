package com.stromblex.vartapack.util;

import java.net.URI;

public final class UrlUtil {
    private UrlUtil() {}

    /**
     * Returns true only for safe http/https URLs. Refuses {@code file:},
     * {@code jar:}, {@code javascript:} and similar schemes so that the
     * "Open Support Page" button cannot be abused via a malicious profile.
     */
    public static boolean isSafeWebUrl(String url) {
        if (url == null || url.isBlank()) return false;
        try {
            URI uri = URI.create(url.trim());
            String scheme = uri.getScheme();
            if (scheme == null) return false;
            scheme = scheme.toLowerCase();
            return (scheme.equals("http") || scheme.equals("https")) && uri.getHost() != null;
        } catch (Exception e) {
            return false;
        }
    }
}
