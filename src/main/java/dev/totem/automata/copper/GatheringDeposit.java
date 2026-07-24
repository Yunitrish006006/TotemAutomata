package dev.totem.automata.copper;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Atomic-capacity simulation and insertion for returning gathered items home. */
public final class GatheringDeposit {
    private GatheringDeposit() { }
    public static boolean canInsertAll(Container container, List<ItemStack> stacks) {
        List<ItemStack> simulated = new ArrayList<>(container.getContainerSize());
        for (int slot = 0; slot < container.getContainerSize(); slot++) simulated.add(container.getItem(slot).copy());
        for (ItemStack stack : stacks) { ItemStack remaining = stack.copy(); simulate(container, simulated, remaining); if (!remaining.isEmpty()) return false; }
        return true;
    }
    public static boolean insertAll(Container container, List<ItemStack> stacks) {
        for (ItemStack stack : stacks) {
            ItemStack remaining = stack.copy();
            for (int slot = 0; slot < container.getContainerSize() && !remaining.isEmpty(); slot++) merge(container, slot, remaining);
            for (int slot = 0; slot < container.getContainerSize() && !remaining.isEmpty(); slot++) place(container, slot, remaining);
            if (!remaining.isEmpty()) return false;
        }
        return true;
    }
    public static boolean shouldReturnHome(ItemStack storage, CopperGolemActivity activity) {
        return GatheringStorage.full(storage) || activity == CopperGolemActivity.RETURNING_HOME || activity == CopperGolemActivity.DEPOSITING || activity == CopperGolemActivity.BLOCKED_NO_VALID_TARGET;
    }
    private static void simulate(Container container, List<ItemStack> slots, ItemStack remaining) {
        for (int slot = 0; slot < slots.size() && !remaining.isEmpty(); slot++) {
            ItemStack existing = slots.get(slot); if (existing.isEmpty() || !ItemStack.isSameItemSameComponents(existing, remaining) || !container.canPlaceItem(slot, remaining)) continue;
            int moved = Math.min(remaining.getCount(), Math.min(existing.getMaxStackSize(), container.getMaxStackSize(remaining)) - existing.getCount());
            if (moved > 0) { existing.grow(moved); remaining.shrink(moved); slots.set(slot, existing); }
        }
        for (int slot = 0; slot < slots.size() && !remaining.isEmpty(); slot++) {
            if (!slots.get(slot).isEmpty() || !container.canPlaceItem(slot, remaining)) continue;
            int moved = Math.min(remaining.getCount(), container.getMaxStackSize(remaining)); slots.set(slot, remaining.copyWithCount(moved)); remaining.shrink(moved);
        }
    }
    private static void merge(Container container, int slot, ItemStack remaining) {
        ItemStack existing = container.getItem(slot); if (existing.isEmpty() || !ItemStack.isSameItemSameComponents(existing, remaining) || !container.canPlaceItem(slot, remaining)) return;
        int moved = Math.min(remaining.getCount(), Math.min(existing.getMaxStackSize(), container.getMaxStackSize(remaining)) - existing.getCount());
        if (moved > 0) { existing.grow(moved); remaining.shrink(moved); container.setItem(slot, existing); }
    }
    private static void place(Container container, int slot, ItemStack remaining) {
        if (!container.getItem(slot).isEmpty() || !container.canPlaceItem(slot, remaining)) return;
        int moved = Math.min(remaining.getCount(), container.getMaxStackSize(remaining)); if (moved > 0) { container.setItem(slot, remaining.copyWithCount(moved)); remaining.shrink(moved); }
    }
}
