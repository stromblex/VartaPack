package com.stromblex.vartapack.api;

public record EnvironmentInfo(
        String minecraftVersion,
        String loaderName,
        String loaderVersion,
        String javaVersion,
        int javaMajor,
        String osName,
        String osVersion,
        long maxMemoryMb,
        String gameDirectory
) {
    public static EnvironmentInfo capture(Platform platform) {
        String javaVer = System.getProperty("java.version", "unknown");
        int javaMajor = parseJavaMajor(javaVer);
        long maxMem = Runtime.getRuntime().maxMemory() / (1024L * 1024L);
        return new EnvironmentInfo(
                platform.getMinecraftVersion(),
                platform.getLoaderName(),
                platform.getLoaderVersion(),
                javaVer,
                javaMajor,
                System.getProperty("os.name", "unknown"),
                System.getProperty("os.version", "unknown"),
                maxMem,
                platform.getGameDirectory().toString()
        );
    }

    private static int parseJavaMajor(String javaVersion) {
        try {
            String v = javaVersion;
            if (v.startsWith("1.")) v = v.substring(2);
            int dot = v.indexOf('.');
            int dash = v.indexOf('-');
            int end = v.length();
            if (dot >= 0) end = Math.min(end, dot);
            if (dash >= 0) end = Math.min(end, dash);
            return Integer.parseInt(v.substring(0, end));
        } catch (Exception e) {
            return -1;
        }
    }
}
