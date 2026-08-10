package com.azscompanions.client;

/**
 * Loader-agnostic vertical scrollbar math with click-drag thumb support.
 * Screens render the track/thumb; this class owns scroll position and drag state.
 */
public final class GuiScrollbarState {
    private int scroll;
    private int maxScroll;
    private int viewTop;
    private int viewBottom;
    private int trackX;
    private int trackW = 4;
    private boolean dragging;
    private int dragGrabOffset;

    public void layout(int trackX, int viewTop, int viewBottom, int trackW, int maxScroll) {
        this.trackX = trackX;
        this.viewTop = viewTop;
        this.viewBottom = viewBottom;
        this.trackW = Math.max(2, trackW);
        this.maxScroll = Math.max(0, maxScroll);
        this.scroll = clamp(scroll, 0, this.maxScroll);
        if (this.maxScroll <= 0) {
            dragging = false;
        }
    }

    public int scroll() {
        return scroll;
    }

    public int maxScroll() {
        return maxScroll;
    }

    public boolean isDragging() {
        return dragging;
    }

    public void setScroll(int value) {
        scroll = clamp(value, 0, maxScroll);
    }

    public void scrollBy(int delta) {
        setScroll(scroll + delta);
    }

    public int trackH() {
        return Math.max(1, viewBottom - viewTop);
    }

    public int thumbH() {
        int trackH = trackH();
        return Math.max(16, (int) (trackH * (trackH / (float) (trackH + Math.max(1, maxScroll)))));
    }

    public int thumbY() {
        int travel = trackH() - thumbH();
        if (travel <= 0 || maxScroll <= 0) {
            return viewTop;
        }
        return viewTop + (int) (travel * (scroll / (float) maxScroll));
    }

    public boolean isOverTrack(double mouseX, double mouseY) {
        return maxScroll > 0
                && mouseX >= trackX && mouseX <= trackX + trackW
                && mouseY >= viewTop && mouseY <= viewBottom;
    }

    public boolean isOverThumb(double mouseX, double mouseY) {
        int ty = thumbY();
        int th = thumbH();
        return maxScroll > 0
                && mouseX >= trackX && mouseX <= trackX + trackW
                && mouseY >= ty && mouseY <= ty + th;
    }

    /** Left-click on track/thumb. Returns true if consumed. */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || maxScroll <= 0 || !isOverTrack(mouseX, mouseY)) {
            return false;
        }
        int ty = thumbY();
        int th = thumbH();
        if (mouseY >= ty && mouseY <= ty + th) {
            dragging = true;
            dragGrabOffset = (int) mouseY - ty;
        } else {
            // Jump so the thumb centers on the click, then start drag.
            jumpToMouseY(mouseY);
            dragging = true;
            dragGrabOffset = thumbH() / 2;
        }
        return true;
    }

    public boolean mouseDragged(double mouseY) {
        if (!dragging || maxScroll <= 0) {
            return false;
        }
        int travel = trackH() - thumbH();
        if (travel <= 0) {
            return true;
        }
        int desiredThumbTop = (int) mouseY - dragGrabOffset;
        float rel = (desiredThumbTop - viewTop) / (float) travel;
        setScroll((int) (rel * maxScroll));
        return true;
    }

    public boolean mouseReleased(int button) {
        if (button == 0 && dragging) {
            dragging = false;
            return true;
        }
        return false;
    }

    private void jumpToMouseY(double mouseY) {
        int travel = trackH() - thumbH();
        if (travel <= 0) {
            setScroll(0);
            return;
        }
        int desiredThumbTop = (int) mouseY - thumbH() / 2;
        float rel = (desiredThumbTop - viewTop) / (float) travel;
        setScroll((int) (rel * maxScroll));
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
