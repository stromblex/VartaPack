package com.stromblex.vartapack.check;

public record CheckResult(
        Severity severity,
        String code,
        String title,
        String message,
        String technicalDetails,
        boolean visibleToPlayer
) {
    public static CheckResult of(Severity severity, String code, String title, String message) {
        return new CheckResult(severity, code, title, message, "", true);
    }

    public CheckResult withDetails(String details) {
        return new CheckResult(severity, code, title, message, details == null ? "" : details, visibleToPlayer);
    }
}
