package dev.totem.automata.containersafety;

import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RemnantBackpackBridgeTest {
    @Test void standaloneFallbackDoesNotClaimEmptyStacksAreBackpacks() {
        assertFalse(RemnantBackpackBridge.isBackpack(ItemStack.EMPTY));
        assertFalse(RemnantBackpackBridge.isSortableTieredBackpack(ItemStack.EMPTY));
        assertEquals(0, RemnantBackpackBridge.tieredBackpackSlots(ItemStack.EMPTY));
    }
}
