package com.stromblex.vartapack.api;

/**
 * Loader-/runtime-agnostic clipboard hook. Loader modules provide
 * an implementation that calls into the Minecraft client clipboard.
 */
public interface ClipboardService {
    void copy(String text);
}
