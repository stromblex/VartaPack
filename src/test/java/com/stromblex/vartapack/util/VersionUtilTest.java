package com.stromblex.vartapack.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class VersionUtilTest {
    @Test
    void supportsMavenStyleRanges() {
        assertTrue(VersionUtil.satisfies("1.5.0", "[1.2,)"));
        assertTrue(VersionUtil.satisfies("1.5.0", "[1.2,2.0)"));
        assertTrue(VersionUtil.satisfies("2.0.0", "(,2.0.0]"));
        assertFalse(VersionUtil.satisfies("2.0.0", "[1.2,2.0)"));
    }

    @Test
    void supportsPredicateChainsAndAlternatives() {
        assertTrue(VersionUtil.satisfies("1.5.0", ">=1.2 <2.0"));
        assertFalse(VersionUtil.satisfies("2.1.0", ">=1.2 <2.0"));
        assertTrue(VersionUtil.satisfies("3.0.0", "[1.0,2.0) || >=3.0"));
    }

    @Test
    void understandsPreReleaseOrdering() {
        assertTrue(VersionUtil.compare("1.0.0-beta", "1.0.0-rc.1") < 0);
        assertTrue(VersionUtil.compare("1.0.0-rc.1", "1.0.0") < 0);
        assertTrue(VersionUtil.satisfies("1.0.0", ">1.0.0-rc.1"));
    }

    @Test
    void supportsCaretAndTildeRanges() {
        assertTrue(VersionUtil.satisfies("1.4.2", "^1.2.0"));
        assertFalse(VersionUtil.satisfies("2.0.0", "^1.2.0"));
        assertTrue(VersionUtil.satisfies("1.2.9", "~1.2.0"));
        assertFalse(VersionUtil.satisfies("1.3.0", "~1.2.0"));
    }
}