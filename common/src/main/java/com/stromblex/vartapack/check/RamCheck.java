package com.stromblex.vartapack.check;

import java.util.ArrayList;
import java.util.List;

public final class RamCheck implements Check {
    @Override public String id() { return "ram"; }

    @Override
    public List<CheckResult> run(CheckContext ctx) {
        long mb = ctx.environment().maxMemoryMb();
        long min = ctx.profile().minimumRamMb();
        long rec = ctx.profile().recommendedRamMb();
        List<CheckResult> out = new ArrayList<>();
        if (min > 0 && mb < min) {
            out.add(CheckResult.of(Severity.ERROR,
                    "ram.tooLow",
                    "Allocated RAM is below minimum",
                    "Allocated RAM: " + mb + " MB, minimum required: " + min + " MB."));
        } else if (rec > 0 && mb < rec) {
            out.add(CheckResult.of(Severity.WARNING,
                    "ram.belowRecommended",
                    "Allocated RAM is below recommended",
                    "Allocated RAM: " + mb + " MB, recommended: " + rec + " MB."));
        }
        return out;
    }
}
