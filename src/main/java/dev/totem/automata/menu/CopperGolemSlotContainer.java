package dev.totem.automata.menu;

import net.minecraft.world.Container;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Live three-slot Copper Golem container backed only by the menu authority. */
public final class CopperGolemSlotContainer implements Container {
    public static final int FUEL = 0, GATHERING_TOOL = 1, GATHERING_STORAGE = 2, SIZE = 3;
    private final CopperGolem golem;
    private final ServerPlayer viewer;
    private final CopperGolemMenuAuthority authority;
    public CopperGolemSlotContainer(CopperGolem golem, ServerPlayer viewer, CopperGolemMenuAuthority authority) {
        this.golem = golem; this.viewer = viewer; this.authority = authority;
    }
    @Override public int getContainerSize() { return SIZE; }
    @Override public boolean isEmpty() { for (int slot = 0; slot < SIZE; slot++) if (!getItem(slot).isEmpty()) return false; return true; }
    @Override public ItemStack getItem(int slot) { return switch (slot) {
        case FUEL -> authority.fuel(golem); case GATHERING_TOOL -> authority.gatheringTool(golem); case GATHERING_STORAGE -> authority.gatheringStorage(golem); default -> ItemStack.EMPTY; }; }
    @Override public ItemStack removeItem(int slot, int amount) { ItemStack stack = getItem(slot); if (stack.isEmpty() || amount <= 0) return ItemStack.EMPTY; ItemStack removed = stack.split(amount); setItem(slot, stack); return removed; }
    @Override public ItemStack removeItemNoUpdate(int slot) { ItemStack stack = getItem(slot); setItem(slot, ItemStack.EMPTY); return stack; }
    @Override public void setItem(int slot, ItemStack stack) { switch (slot) { case FUEL -> authority.setFuel(golem, stack); case GATHERING_TOOL -> authority.setGatheringTool(golem, stack); case GATHERING_STORAGE -> authority.setGatheringStorage(golem, stack); default -> { } } setChanged(); }
    @Override public void setChanged() { if (viewer != null) authority.refresh(viewer, golem); }
    @Override public boolean stillValid(Player player) { return authority.canUse(player, golem); }
    @Override public void clearContent() { for (int slot = 0; slot < SIZE; slot++) setItem(slot, ItemStack.EMPTY); }
}
