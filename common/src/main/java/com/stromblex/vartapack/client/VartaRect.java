package com.stromblex.vartapack.client;

public record VartaRect(int x, int y, int width, int height) {
    public int right() {
        return x + width;
    }

    public int bottom() {
        return y + height;
    }

    public boolean contains(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= right() && mouseY >= y && mouseY <= bottom();
    }

    public VartaRect inset(int amount) {
        int inset = Math.max(0, amount);
        return new VartaRect(x + inset, y + inset, Math.max(1, width - inset * 2), Math.max(1, height - inset * 2));
    }
}
