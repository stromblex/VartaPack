package com.stromblex.vartapack.client;

public record VartaScreenMetrics(
        int width,
        int height,
        VartaLayoutMode mode,
        int margin,
        int gap,
        int headerHeight,
        VartaRect frame
) {
    public int edgePadding() {
        return margin;
    }

    public VartaRect headerBounds() {
        return new VartaRect(frame.x(), frame.y(), frame.width(), headerHeight);
    }

    public VartaRect contentBounds(int bottomReserved) {
        int y = frame.y() + headerHeight + margin;
        int bottom = frame.bottom() - Math.max(0, bottomReserved);
        return new VartaRect(frame.x() + margin, y, Math.max(1, frame.width() - margin * 2), Math.max(1, bottom - y));
    }
}
