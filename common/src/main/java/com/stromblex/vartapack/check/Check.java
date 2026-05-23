package com.stromblex.vartapack.check;

import java.util.List;

public interface Check {
    /** Stable identifier used for logs and report codes. */
    String id();
    List<CheckResult> run(CheckContext context);
}
