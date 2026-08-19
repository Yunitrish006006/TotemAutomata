package dev.totem.automata.copper;

import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Shared-capacity carried storage for gathering drops. */
public final class GatheringStorage {
    public static final int MAX_STACK_SIZE = 16;
    private static final String LEGACY_KEY = "deadrecall_gathering_storage_stack";
    private static final String SLOT_PREFIX = "deadrecall_gathering_storage_slot_";

    private GatheringStorage() { }

    /** Normalizes mixed drops while enforcing only the shared total-item limit. */
    public static Optional<List<ItemStack>> normalizeDrops(List<ItemStack> rawDrops) {
        List<ItemStack> normalized = new ArrayList<>();
        int total = 0;
        for (ItemStack drop : rawDrops) {
            if (drop.isEmpty()) continue;
            total += drop.getCount();
            if (total > MAX_STACK_SIZE) return Optional.empty();
            merge(normalized, drop);
        }
        return normalized.isEmpty() ? Optional.empty() : Optional.of(copy(normalized));
    }

    public static List<ItemStack> read(CompoundTag tag, RegistryAccess registryAccess) {
        List<ItemStack> stacks = new ArrayList<>();
        for (int slot = 0; slot < MAX_STACK_SIZE; slot++) {
            ItemStack stack = CopperGolemData.readItemStack(tag, SLOT_PREFIX + slot, registryAccess);
            if (!stack.isEmpty()) stacks.add(stack);
        }
        if (stacks.isEmpty()) {
            ItemStack legacy = CopperGolemData.readItemStack(tag, LEGACY_KEY, registryAccess);
            if (!legacy.isEmpty()) stacks.add(legacy);
        }
        return copy(stacks);
    }

    public static void write(CompoundTag tag, List<ItemStack> stacks, RegistryAccess registryAccess) {
        tag.remove(LEGACY_KEY);
        for (int slot = 0; slot < MAX_STACK_SIZE; slot++) tag.remove(SLOT_PREFIX + slot);
        List<ItemStack> normalized = new ArrayList<>();
        for (ItemStack stack : stacks) if (!stack.isEmpty()) merge(normalized, stack);
        if (!normalized.isEmpty()) {
            CopperGolemData.writeItemStack(tag, LEGACY_KEY, normalized.getFirst(), registryAccess);
        }
        int slot = 0;
        for (ItemStack stack : normalized) {
            if (slot >= MAX_STACK_SIZE) break;
            CopperGolemData.writeItemStack(tag, SLOT_PREFIX + slot++, stack, registryAccess);
        }
    }

    public static boolean canStore(List<ItemStack> storage, List<ItemStack> drops) {
        return totalCount(storage) + totalCount(drops) <= MAX_STACK_SIZE;
    }

    public static List<ItemStack> addDrops(List<ItemStack> storage, List<ItemStack> drops) {
        if (!canStore(storage, drops)) return copy(storage);
        List<ItemStack> result = new ArrayList<>(copy(storage));
        for (ItemStack drop : drops) if (!drop.isEmpty()) merge(result, drop);
        return copy(result);
    }

    public static boolean full(List<ItemStack> storage) {
        return totalCount(storage) >= MAX_STACK_SIZE;
    }

    public static int totalCount(List<ItemStack> stacks) {
        int total = 0;
        for (ItemStack stack : stacks) if (!stack.isEmpty()) total += stack.getCount();
        return total;
    }

    public static ItemStack displayStack(List<ItemStack> stacks) {
        return stacks.stream().filter(stack -> !stack.isEmpty()).findFirst().map(ItemStack::copy).orElse(ItemStack.EMPTY);
    }

    private static void merge(List<ItemStack> stacks, ItemStack incoming) {
        ItemStack remaining = incoming.copy();
        for (ItemStack existing : stacks) {
            if (remaining.isEmpty()) break;
            if (!ItemStack.isSameItemSameComponents(existing, remaining)) continue;
            int room = existing.getMaxStackSize() - existing.getCount();
            if (room <= 0) continue;
            int moved = Math.min(room, remaining.getCount());
            existing.grow(moved);
            remaining.shrink(moved);
        }
        while (!remaining.isEmpty()) {
            int moved = Math.min(remaining.getCount(), remaining.getMaxStackSize());
            stacks.add(remaining.copyWithCount(moved));
            remaining.shrink(moved);
        }
    }

    private static List<ItemStack> copy(List<ItemStack> stacks) {
        return stacks.stream().filter(stack -> !stack.isEmpty()).map(ItemStack::copy).toList();
    }
}
