package com.stromblex.vartapack.util;

import java.nio.file.Path;
import java.nio.file.Paths;

public final class PathUtil {
    private PathUtil() {}

    public static Path safe(String s) {
        try { return Paths.get(s); }
        catch (Exception e) { return Paths.get("."); }
    }
}
