package com.stromblex.vartapack.client;

public final class VartaUiLayout {
    public static final int EDGE_PADDING_NORMAL = 12;
    public static final int EDGE_PADDING_COMPACT = 8;
    public static final int EDGE_PADDING_NARROW = 6;
    public static final int GAP_LARGE = 16;
    public static final int GAP_MEDIUM = 12;
    public static final int GAP_SMALL = 8;
    public static final int SCROLLBAR_GUTTER = 8;

    private VartaUiLayout() {
    }

    public static VartaScreenMetrics metrics(int width, int height) {
        VartaLayoutMode mode = modeFor(width, height);
        int padding = edgePadding(mode);
        int gap = gap(mode);
        int headerHeight = headerHeight(mode);
        VartaRect root = new VartaRect(0, 0, Math.max(1, width), Math.max(1, height));
        return new VartaScreenMetrics(width, height, mode, padding, gap, headerHeight, root);
    }

    public static VartaLayoutMode modeFor(int width, int height) {
        if (width >= 900 && height >= 520) {
            return VartaLayoutMode.NORMAL;
        }
        if (width >= 640 && height >= 360) {
            return VartaLayoutMode.COMPACT;
        }
        return VartaLayoutMode.NARROW;
    }

    public static int edgePadding(VartaLayoutMode mode) {
        return switch (mode) {
            case NORMAL -> EDGE_PADDING_NORMAL;
            case COMPACT -> EDGE_PADDING_COMPACT;
            case NARROW -> EDGE_PADDING_NARROW;
        };
    }

    public static int gap(VartaLayoutMode mode) {
        return switch (mode) {
            case NORMAL -> GAP_LARGE;
            case COMPACT -> GAP_MEDIUM;
            case NARROW -> GAP_SMALL;
        };
    }

    public static int headerHeight(VartaLayoutMode mode) {
        return switch (mode) {
            case NORMAL -> 58;
            case COMPACT -> 54;
            case NARROW -> 50;
        };
    }

    public static int columnsFor(VartaLayoutMode mode) {
        return switch (mode) {
            case NORMAL -> 4;
            case COMPACT -> 2;
            case NARROW -> 1;
        };
    }

    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }

    public static int buttonWidth(int availableWidth, int min, int max) {
        if (availableWidth <= min) {
            return Math.max(1, availableWidth);
        }
        return clamp(availableWidth, min, Math.min(max, availableWidth));
    }

    public static int textColor(int color) {
        return (color & 0xFF000000) == 0 ? color | 0xFF000000 : color;
    }
}
