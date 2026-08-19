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
    public CopperGolemSlotContainer(CopperGolem golem, ServerPlayer viewer, CopperGolemMenuAuthority authority) {
        this.golem = golem; this.viewer = viewer; this.authority = authority;
    }
    @Override public int getContainerSize() { return SIZE; }
    @Override public boolean isEmpty() { for (int slot = 0; slot < SIZE; slot++) if (!getItem(slot).isEmpty()) return false; return true; }
    @Override public ItemStack getItem(int slot) {
        if (slot == FUEL) return authority.fuel(golem);
        if (slot == GATHERING_TOOL) return authority.gatheringTool(golem);
        int storageIndex = slot - GATHERING_STORAGE_START;
        if (storageIndex < 0 || storageIndex >= GATHERING_STORAGE_SLOTS) return ItemStack.EMPTY;
        List<ItemStack> storage = authority.gatheringStorage(golem);
        return storageIndex < storage.size() ? storage.get(storageIndex).copy() : ItemStack.EMPTY;
    }
    @Override public ItemStack removeItem(int slot, int amount) {
        ItemStack stack = getItem(slot); if (stack.isEmpty() || amount <= 0) return ItemStack.EMPTY;
        ItemStack removed = stack.split(amount); setItem(slot, stack); return removed;
    }
    @Override public ItemStack removeItemNoUpdate(int slot) { ItemStack stack = getItem(slot); setItem(slot, ItemStack.EMPTY); return stack; }
    @Override public void setItem(int slot, ItemStack stack) {
        if (slot == FUEL) authority.setFuel(golem, stack);
        else if (slot == GATHERING_TOOL) authority.setGatheringTool(golem, stack);
        else {
            int storageIndex = slot - GATHERING_STORAGE_START;
            if (storageIndex >= 0 && storageIndex < GATHERING_STORAGE_SLOTS) {
                List<ItemStack> storage = new ArrayList<>(authority.gatheringStorage(golem));
                while (storage.size() <= storageIndex) storage.add(ItemStack.EMPTY);
                storage.set(storageIndex, stack.copy());
                storage.removeIf(ItemStack::isEmpty);
                authority.setGatheringStorage(golem, storage);
            }
        }
        setChanged();
    }
    @Override public void setChanged() { if (viewer != null) authority.refresh(viewer, golem); }
    @Override public boolean stillValid(Player player) { return authority.canUse(player, golem); }
    @Override public void clearContent() {
        authority.setFuel(golem, ItemStack.EMPTY);
        authority.setGatheringTool(golem, ItemStack.EMPTY);
        authority.setGatheringStorage(golem, List.of());
        setChanged();
    }
}
