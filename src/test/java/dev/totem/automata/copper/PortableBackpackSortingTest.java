package dev.totem.automata.copper;

import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PortableBackpackSortingTest {
    @Test void standaloneAutomataNeverTreatsAnEmptyStackAsASortableDestination() {
        assertFalse(PortableBackpackSorting.isSortableDestination(ItemStack.EMPTY));
        assertFalse(PortableBackpackSorting.canSortInto(ItemStack.EMPTY, ItemStack.EMPTY));
        assertTrue(PortableBackpackSorting.insert(ItemStack.EMPTY, ItemStack.EMPTY).isEmpty());
    }
}
