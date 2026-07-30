package dev.totem.automata.copper;

import dev.totem.automata.containersafety.ContainerSafetyBridge;
import dev.totem.automata.containersafety.RemnantBackpackBridge;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/** Component-safe, optional-Remnant operations for tiered backpack destinations. */
public final class PortableBackpackSorting {
    private PortableBackpackSorting() { }

    public static boolean isSortableDestination(ItemStack stack) {
        return RemnantBackpackBridge.isSortableTieredBackpack(stack);
    }

    public static boolean canSortInto(ItemStack backpack, ItemStack carried) {
        return isSortableDestination(backpack) && mayInsert(carried)
                && BackpackSortingHelper.canSortInto(load(backpack), carried);
    }

    public static boolean canPlaceSomewhere(ItemStack backpack, ItemStack carried) {
        return isSortableDestination(backpack) && mayInsert(carried)
                && BackpackSortingHelper.canPlaceSomewhere(load(backpack), carried);
    }

    /** Inserts only when the optional portable-container policy permits it. */
    public static ItemStack insert(ItemStack backpack, ItemStack carried) {
        if (!isSortableDestination(backpack) || !mayInsert(carried)) return carried.copy();
        NonNullList<ItemStack> items = load(backpack);
        ItemStack remaining = BackpackSortingHelper.insertInto(items, carried);
        backpack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
        return remaining;
    }

    private static boolean mayInsert(ItemStack carried) {
        return !RemnantBackpackBridge.isBackpack(carried)
                && ContainerSafetyBridge.mayInsertIntoBackpack(carried);
    }

    private static NonNullList<ItemStack> load(ItemStack backpack) {
        int size = RemnantBackpackBridge.tieredBackpackSlots(backpack);
        NonNullList<ItemStack> items = NonNullList.withSize(Math.max(0, size), ItemStack.EMPTY);
        backpack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(items);
        return items;
    }
}
