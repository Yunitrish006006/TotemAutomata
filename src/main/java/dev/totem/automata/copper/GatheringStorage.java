package dev.totem.automata.copper;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Legacy 16-item carried storage rules for gathering drops. */
public final class GatheringStorage {
    public static final int MAX_STACK_SIZE = 16;
    private GatheringStorage() { }
    /** Accepts exactly one item/component kind and a total count no greater than sixteen. */
    public static Optional<List<ItemStack>> normalizeDrops(List<ItemStack> rawDrops) {
        List<ItemStack> normalized = new ArrayList<>(); int total = 0;
        for (ItemStack drop : rawDrops) {
            if (drop.isEmpty()) continue;
            if (!normalized.isEmpty() && !ItemStack.isSameItemSameComponents(normalized.getFirst(), drop)) return Optional.empty();
            if (normalized.isEmpty()) normalized.add(drop.copy()); else normalized.getFirst().grow(drop.getCount());
            if ((total += drop.getCount()) > MAX_STACK_SIZE) return Optional.empty();
        }
        return normalized.isEmpty() ? Optional.empty() : Optional.of(List.copyOf(normalized));
    }
    public static boolean canStore(ItemStack storage, List<ItemStack> drops) {
        ItemStack simulated = storage.copy();
        for (ItemStack drop : drops) {
            if (drop.isEmpty()) continue;
            if (simulated.isEmpty()) { if (drop.getCount() > MAX_STACK_SIZE) return false; simulated = drop.copy(); continue; }
            if (!ItemStack.isSameItemSameComponents(simulated, drop) || simulated.getCount() + drop.getCount() > MAX_STACK_SIZE) return false;
            simulated.grow(drop.getCount());
        }
        return !simulated.isEmpty();
    }
    public static ItemStack addDrops(ItemStack storage, List<ItemStack> drops) {
        ItemStack result = storage.copy();
        for (ItemStack drop : drops) {
            if (drop.isEmpty()) continue;
            if (result.isEmpty()) result = drop.copy(); else if (ItemStack.isSameItemSameComponents(result, drop)) result.grow(drop.getCount());
        }
        if (!result.isEmpty() && result.getCount() > MAX_STACK_SIZE) result.setCount(MAX_STACK_SIZE);
        return result;
    }
    public static boolean full(ItemStack storage) { return !storage.isEmpty() && storage.getCount() >= MAX_STACK_SIZE; }
}
