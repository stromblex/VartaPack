package com.stromblex.vartapack.doctor;

/**
 * Options for doctor mode execution.
 */
public record DoctorOptions(
        String instancePath,
        boolean jsonOutput,
        boolean verbose
) {
    public DoctorOptions {
        if (instancePath == null) instancePath = ".";
    }

    public static DoctorOptions defaults(String path) {
        return new DoctorOptions(path, false, false);
    }
}
