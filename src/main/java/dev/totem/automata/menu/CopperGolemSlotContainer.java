package dev.totem.automata.menu;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Live Copper Golem container with fuel, tool, and sixteen carried-item display slots. */
public final class CopperGolemSlotContainer implements Container {
    public static final int FUEL = 0, GATHERING_TOOL = 1, GATHERING_STORAGE_START = 2, GATHERING_STORAGE_SLOTS = 16;
    public static final int SIZE = GATHERING_STORAGE_START + GATHERING_STORAGE_SLOTS;
    private final CopperGolem golem;
    private final ServerPlayer viewer;
    private final CopperGolemMenuAuthority authority;
    private final ItemStack[] items = new ItemStack[SIZE];
    private final ItemStack[] committed = new ItemStack[SIZE];
    private ItemStack persistedFuel = ItemStack.EMPTY, persistedTool = ItemStack.EMPTY;
    private List<ItemStack> persistedStorage = List.of();
    public CopperGolemSlotContainer(CopperGolem golem, ServerPlayer viewer, CopperGolemMenuAuthority authority) {
        this.golem = golem; this.viewer = viewer; this.authority = authority;
        java.util.Arrays.fill(items, ItemStack.EMPTY);
        java.util.Arrays.fill(committed, ItemStack.EMPTY);
        synchronizeAuthority();
    }
    @Override public int getContainerSize() { return SIZE; }
    @Override public boolean isEmpty() { for (int slot = 0; slot < SIZE; slot++) if (!getItem(slot).isEmpty()) return false; return true; }
    @Override public ItemStack getItem(int slot) {
        if (slot < 0 || slot >= SIZE) return ItemStack.EMPTY;
        synchronizeAuthority();
        return items[slot];
    }
    @Override public ItemStack removeItem(int slot, int amount) {
        ItemStack stack = getItem(slot); if (stack.isEmpty() || amount <= 0) return ItemStack.EMPTY;
        ItemStack removed = stack.split(amount); setItem(slot, stack); return removed;
    }
    @Override public ItemStack removeItemNoUpdate(int slot) { ItemStack stack = getItem(slot); setItem(slot, ItemStack.EMPTY); return stack; }
    @Override public void setItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= SIZE) return;
        synchronizeAuthority();
        items[slot] = stack;
        setChanged();
    }
    @Override public void setChanged() {
        synchronizeAuthority();
        if (!ItemStack.matches(items[FUEL], committed[FUEL])) {
            authority.setFuel(golem, items[FUEL]);
            persistedFuel = authority.fuel(golem).copy();
            committed[FUEL] = items[FUEL].copy();
        }
        if (!ItemStack.matches(items[GATHERING_TOOL], committed[GATHERING_TOOL])) {
            authority.setGatheringTool(golem, items[GATHERING_TOOL]);
            persistedTool = authority.gatheringTool(golem).copy();
            committed[GATHERING_TOOL] = items[GATHERING_TOOL].copy();
        }
        boolean storageChanged = false;
        for (int slot = GATHERING_STORAGE_START; slot < SIZE; slot++) {
            storageChanged |= !ItemStack.matches(items[slot], committed[slot]);
        }
        if (storageChanged) {
            List<ItemStack> storage = new ArrayList<>();
            for (int slot = GATHERING_STORAGE_START; slot < SIZE; slot++) storage.add(items[slot].copy());
            authority.setGatheringStorage(golem, storage);
            persistedStorage = copy(authority.gatheringStorage(golem));
            for (int slot = GATHERING_STORAGE_START; slot < SIZE; slot++) committed[slot] = items[slot].copy();
        }
        if (viewer != null) authority.refresh(viewer, golem);
    }

    /** Keep vanilla's mutable stack references until the authority actually changes.
     * Our own storage writes compact persisted data, but must not move live slots
     * while vanilla is still iterating a QUICK_MOVE or PICKUP_ALL operation. */
    private void synchronizeAuthority() {
        ItemStack fuel = authority.fuel(golem);
        if (!ItemStack.matches(fuel, persistedFuel)) {
            items[FUEL] = fuel.copy();
            committed[FUEL] = fuel.copy();
            persistedFuel = fuel.copy();
        }
        ItemStack tool = authority.gatheringTool(golem);
        if (!ItemStack.matches(tool, persistedTool)) {
            items[GATHERING_TOOL] = tool.copy();
            committed[GATHERING_TOOL] = tool.copy();
            persistedTool = tool.copy();
        }
        List<ItemStack> storage = authority.gatheringStorage(golem);
        if (!matches(storage, persistedStorage)) {
            for (int index = 0; index < GATHERING_STORAGE_SLOTS; index++) {
                ItemStack stack = index < storage.size() ? storage.get(index) : ItemStack.EMPTY;
                items[GATHERING_STORAGE_START + index] = stack.copy();
                committed[GATHERING_STORAGE_START + index] = stack.copy();
            }
            persistedStorage = copy(storage);
        }
    }
    private static boolean matches(List<ItemStack> first, List<ItemStack> second) {
        if (first.size() != second.size()) return false;
        for (int index = 0; index < first.size(); index++) {
            if (!ItemStack.matches(first.get(index), second.get(index))) return false;
        }
        return true;
    }
    private static List<ItemStack> copy(List<ItemStack> stacks) { return stacks.stream().map(ItemStack::copy).toList(); }
    @Override public boolean stillValid(Player player) { return authority.canUse(player, golem); }
    @Override public void clearContent() {
        synchronizeAuthority();
        java.util.Arrays.fill(items, ItemStack.EMPTY);
        setChanged();
    }
}
