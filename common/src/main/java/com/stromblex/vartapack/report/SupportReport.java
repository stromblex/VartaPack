package com.stromblex.vartapack.report;

public record SupportReport(String markdown, int critical, int errors, int warnings, int infos) {
}
