package dev.totem.automata.copper;

import dev.totem.automata.containersafety.ContainerSafetyBridge;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/**
 * Module-owned destination selection and insertion for ordinary containers and
 * optional tiered-backpack slots. It preserves the legacy preference for a
 * compatible nested backpack before an ordinary container insertion.
 */
public final class SortingDestinationService {
    private SortingDestinationService() { }

    public static boolean canAccept(Container destination, ItemStack carried) {
        if (carried.isEmpty() || !ContainerSafetyBridge.mayInsertIntoContainer(destination, carried)) return false;
        return canAcceptDirectly(destination, carried) || findPortableDestination(destination, carried) >= 0;
    }

    public static ItemStack insert(Container destination, ItemStack carried) {
        if (carried.isEmpty() || !ContainerSafetyBridge.mayInsertIntoContainer(destination, carried)) return carried.copy();
        int backpackSlot = findPortableDestination(destination, carried);
        if (backpackSlot >= 0) {
            ItemStack backpack = destination.getItem(backpackSlot);
            ItemStack remaining = PortableBackpackSorting.insert(backpack, carried);
            destination.setItem(backpackSlot, backpack);
            if (remaining.isEmpty()) return ItemStack.EMPTY;
            carried = remaining;
        }
        return insertDirectly(destination, carried);
    }

    /** Whether a destination has any compatible capacity, including an empty ordinary slot. */
    public static boolean hasAvailableSpace(Container destination, ItemStack carried) {
        if (carried.isEmpty() || !ContainerSafetyBridge.mayInsertIntoContainer(destination, carried)) return false;
        for (int slot = 0; slot < destination.getContainerSize(); slot++) {
            ItemStack stack = destination.getItem(slot);
            if (stack.isEmpty() || (ItemStack.isSameItemSameComponents(stack, carried) && stack.getCount() < stack.getMaxStackSize())) return true;
            if (PortableBackpackSorting.canPlaceSomewhere(stack, carried)) return true;
        }
        return false;
    }

    private static int findPortableDestination(Container destination, ItemStack carried) {
        for (int slot = 0; slot < destination.getContainerSize(); slot++) {
            if (PortableBackpackSorting.canSortInto(destination.getItem(slot), carried)) return slot;
        }
        return -1;
    }

    private static boolean canAcceptDirectly(Container destination, ItemStack carried) {
        boolean matching = false, empty = false;
        for (int slot = 0; slot < destination.getContainerSize(); slot++) {
            ItemStack stack = destination.getItem(slot);
            if (stack.isEmpty()) { empty = true; continue; }
            if (!ItemStack.isSameItemSameComponents(stack, carried)) continue;
            matching = true;
            if (stack.getCount() < stack.getMaxStackSize()) return true;
        }
        return matching && empty;
    }

    private static ItemStack insertDirectly(Container destination, ItemStack carried) {
        ItemStack remaining = carried.copy();
        for (int slot = 0; slot < destination.getContainerSize() && !remaining.isEmpty(); slot++) {
            ItemStack stack = destination.getItem(slot);
            if (stack.isEmpty() || !ItemStack.isSameItemSameComponents(stack, remaining)) continue;
            int count = Math.min(remaining.getCount(), stack.getMaxStackSize() - stack.getCount());
            if (count <= 0) continue;
            stack.grow(count); remaining.shrink(count); destination.setItem(slot, stack);
        }
        for (int slot = 0; slot < destination.getContainerSize() && !remaining.isEmpty(); slot++) {
            if (!destination.getItem(slot).isEmpty()) continue;
            int count = Math.min(remaining.getCount(), remaining.getMaxStackSize());
            destination.setItem(slot, remaining.copyWithCount(count)); remaining.shrink(count);
        }
        return remaining.isEmpty() ? ItemStack.EMPTY : remaining;
    }
}
