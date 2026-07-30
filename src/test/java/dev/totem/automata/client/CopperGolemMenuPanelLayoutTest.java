package dev.totem.automata.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CopperGolemMenuPanelLayoutTest {
    @Test
    void centersPreferredAndEnforcesLegacyMinimum() {
        var preferred = CopperGolemMenuPanelLayout.bounds(800, 600);
        assertEquals(520, preferred.width());
        assertEquals(304, preferred.height());

        var small = CopperGolemMenuPanelLayout.bounds(410, 245);
        assertEquals(400, small.width());
        assertEquals(236, small.height());
    }

    @Test
    void minimumHeightKeepsBindingCardRulesAndEditorSeparated() {
        var bounds = CopperGolemMenuPanelLayout.bounds(410, 245);
        int finalCardBottom = CopperGolemMenuPanelLayout.bindingListY(bounds)
                + CopperGolemMenuPanelLayout.visibleBindingRows(bounds) * 28 - 3;

        assertEquals(1, CopperGolemMenuPanelLayout.visibleBindingRows(bounds));
        assertTrue(finalCardBottom < CopperGolemMenuPanelLayout.bindingManualRulesY(bounds));
        assertTrue(CopperGolemMenuPanelLayout.bindingManualRulesY(bounds)
                < CopperGolemMenuPanelLayout.bindingControlsY(bounds));
        assertTrue(CopperGolemMenuPanelLayout.bindingControlsY(bounds) + 58
                < bounds.y() + bounds.height());
    }
}
