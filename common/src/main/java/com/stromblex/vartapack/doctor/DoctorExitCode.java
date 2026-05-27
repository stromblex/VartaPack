package com.stromblex.vartapack.doctor;

/**
 * Exit codes for doctor mode.
 */
public enum DoctorExitCode {
    /** Clean or only INFO-level issues. */
    OK(0),
    /** Warnings or support-limited changes. */
    WARNINGS(1),
    /** Errors or critical issues. */
    ERRORS(2);

    private final int code;

    DoctorExitCode(int code) { this.code = code; }

    public int code() { return code; }
}
