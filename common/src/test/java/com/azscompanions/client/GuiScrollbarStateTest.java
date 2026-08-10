package com.azscompanions.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class GuiScrollbarStateTest {
    @Test
    void clickDragThumbMovesScroll() {
        GuiScrollbarState bar = new GuiScrollbarState();
        bar.layout(100, 10, 110, 4, 100); // track 100px tall, maxScroll 100
        assertTrue(bar.mouseClicked(101, bar.thumbY() + 2, 0));
        assertTrue(bar.isDragging());
        int before = bar.scroll();
        bar.mouseDragged(80);
        assertTrue(bar.scroll() >= before);
        assertTrue(bar.mouseReleased(0));
        assertFalse(bar.isDragging());
    }

    @Test
    void trackClickJumps() {
        GuiScrollbarState bar = new GuiScrollbarState();
        bar.layout(50, 0, 100, 4, 200);
        assertTrue(bar.mouseClicked(51, 90, 0));
        assertTrue(bar.scroll() > 0);
        assertEquals(200, bar.maxScroll());
    }
}
