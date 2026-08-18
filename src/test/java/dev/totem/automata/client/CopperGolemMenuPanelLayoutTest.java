package dev.totem.automata.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CopperGolemMenuPanelLayoutTest {
    @Test
    void centersCompactVanillaInventoryPanel() {
        var preferred = CopperGolemMenuPanelLayout.bounds(800, 600);
        assertEquals(176, preferred.width());
        assertEquals(222, preferred.height());

        var small = CopperGolemMenuPanelLayout.bounds(410, 245);
        assertEquals(176, small.width());
        assertEquals(222, small.height());
        assertTrue(small.x() >= 0);
        assertTrue(small.y() >= 0);
    }
}
