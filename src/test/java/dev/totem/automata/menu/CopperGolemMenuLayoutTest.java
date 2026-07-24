package dev.totem.automata.menu;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CopperGolemMenuLayoutTest {
    @Test void preservesTheThreeLegacyGolemSlotIndices() {
        assertEquals(0, CopperGolemMenuLayout.SLOT_FUEL);
        assertEquals(1, CopperGolemMenuLayout.SLOT_GATHERING_TOOL);
        assertEquals(2, CopperGolemMenuLayout.SLOT_GATHERING_STORAGE);
    }
}
