package dev.totem.automata.copper;

import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class CopperWrenchSelectionTest {
    @Test void ignoresNonWrenchItemsEvenWhenTheyContainTheLegacyKey() {
        ItemStack stack = ItemStack.EMPTY;
        assertFalse(CopperWrenchSelection.isCopperWrench(stack));
        assertNull(CopperWrenchSelection.selectedGolem(stack));
    }

    @Test void exposesCanonicalAndRetainsLegacyIdentifier() {
        assertEquals("totem:automata/copper_wrench", CopperWrenchSelection.ITEM_ID.toString());
        assertEquals("deadrecall:copper_wrench", CopperWrenchSelection.LEGACY_ITEM_ID.toString());
    }
}
