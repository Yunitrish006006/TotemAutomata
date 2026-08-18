package dev.totem.automata.menu;

import dev.totem.automata.copper.GatheringToolPolicy;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/** Module-owned Copper Golem server menu; registration supplies the legacy menu type at cutover. */
public final class CopperGolemMenu extends AbstractContainerMenu {
    private static final int INVENTORY_START = CopperGolemMenuLayout.GOLEM_SLOT_COUNT;
    private static final int INVENTORY_END = INVENTORY_START + 27, HOTBAR_START = INVENTORY_END, HOTBAR_END = HOTBAR_START + 9;
    private final Inventory inventory; private final UUID golemId; private final CopperGolem golem; private final ServerLevel level;
    private final CopperGolemMenuAuthority authority; private boolean gatheringVisible;

    public CopperGolemMenu(MenuType<?> type, int id, Inventory inventory, CopperGolemMenuOpenData data) {
        this(type, id, inventory, null, data.golemId(), null, new SimpleContainer(CopperGolemMenuLayout.GOLEM_SLOT_COUNT), null);
    }
    public CopperGolemMenu(MenuType<?> type, int id, Inventory inventory, Player player, CopperGolem golem, CopperGolemMenuAuthority authority) {
        this(type, id, inventory, golem, golem.getUUID(), golem.level() instanceof ServerLevel server ? server : null,
                new CopperGolemSlotContainer(golem, player instanceof ServerPlayer viewer ? viewer : null, authority), authority);
    }
    private CopperGolemMenu(MenuType<?> type, int id, Inventory inventory, CopperGolem golem, UUID golemId, ServerLevel level, Container slots, CopperGolemMenuAuthority authority) {
        super(type, id); checkContainerSize(slots, CopperGolemMenuLayout.GOLEM_SLOT_COUNT);
        this.inventory = inventory; this.golem = golem; this.golemId = golemId; this.level = level; this.authority = authority;
        addSlot(new FuelSlot(slots, CopperGolemMenuLayout.SLOT_FUEL, CopperGolemMenuLayout.FUEL_SLOT_X, CopperGolemMenuLayout.FUEL_SLOT_Y));
        addSlot(new ToolSlot(slots, CopperGolemMenuLayout.SLOT_GATHERING_TOOL, CopperGolemMenuLayout.GATHERING_TOOL_SLOT_X, CopperGolemMenuLayout.GATHERING_SLOT_Y));
        addSlot(new StorageSlot(slots, CopperGolemMenuLayout.SLOT_GATHERING_STORAGE, CopperGolemMenuLayout.GATHERING_STORAGE_SLOT_X, CopperGolemMenuLayout.GATHERING_SLOT_Y));
        addPlayerSlots(inventory);
    }
    public UUID golemId() { return golemId; }
    /** Uses the shared policy when this client-side menu has no server authority. */
    public boolean canPlaceGatheringTool(ItemStack stack) {
        return authority != null ? authority.isGatheringTool(stack) : GatheringToolPolicy.accepts(stack);
    }
    public void setGatheringSlotsVisible(boolean visible) { gatheringVisible = visible; }
    @Override public boolean stillValid(Player player) { return golem == null || authority.canUse(player, golem); }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY; Slot slot = slots.get(index); if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), original = stack.copy();
        if (index < CopperGolemMenuLayout.GOLEM_SLOT_COUNT) { if (!moveItemStackTo(stack, INVENTORY_START, HOTBAR_END, true)) return ItemStack.EMPTY; }
        else { boolean moved = false; Slot tool = slots.get(CopperGolemMenuLayout.SLOT_GATHERING_TOOL), fuel = slots.get(CopperGolemMenuLayout.SLOT_FUEL);
            if (tool.mayPlace(stack) && !tool.hasItem()) moved = moveItemStackTo(stack, CopperGolemMenuLayout.SLOT_GATHERING_TOOL, CopperGolemMenuLayout.SLOT_GATHERING_TOOL + 1, false);
            if (!moved && fuel.mayPlace(stack)) moved = moveItemStackTo(stack, CopperGolemMenuLayout.SLOT_FUEL, CopperGolemMenuLayout.SLOT_FUEL + 1, false);
            if (!moved && index >= INVENTORY_START && index < INVENTORY_END) moved = moveItemStackTo(stack, HOTBAR_START, HOTBAR_END, false);
            else if (!moved && index >= HOTBAR_START && index < HOTBAR_END) moved = moveItemStackTo(stack, INVENTORY_START, INVENTORY_END, false);
            if (!moved) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY; slot.onTake(player, stack); return original;
    }
    private void addPlayerSlots(Inventory inventory) { for (int row=0; row<3; row++) for (int col=0; col<9; col++) addSlot(new Slot(inventory, col+row*9+9, CopperGolemMenuLayout.PLAYER_INVENTORY_X+col*18, CopperGolemMenuLayout.PLAYER_INVENTORY_Y+row*18)); for (int col=0; col<9; col++) addSlot(new Slot(inventory, col, CopperGolemMenuLayout.PLAYER_INVENTORY_X+col*18, CopperGolemMenuLayout.PLAYER_HOTBAR_Y)); }
    private boolean editable() { return golem == null || authority.canEditGatheringSlots(golem); }
    private boolean visible() { return golem == null ? gatheringVisible : authority.isGatheringMode(golem); }
    private final class FuelSlot extends Slot { FuelSlot(Container c,int s,int x,int y){super(c,s,x,y);} @Override public boolean mayPlace(ItemStack stack){ return level != null ? authority.isFuel(level,stack) : !stack.isEmpty() && inventory.player.level().fuelValues().isFuel(stack); } }
    private final class ToolSlot extends Slot { ToolSlot(Container c,int s,int x,int y){super(c,s,x,y);} @Override public boolean mayPlace(ItemStack stack){return editable() && canPlaceGatheringTool(stack);} @Override public boolean mayPickup(Player p){return editable();} @Override public boolean isActive(){return visible();} @Override public int getMaxStackSize(){return 1;} @Override public int getMaxStackSize(ItemStack stack){return 1;} }
    private final class StorageSlot extends Slot { StorageSlot(Container c,int s,int x,int y){super(c,s,x,y);} @Override public boolean mayPlace(ItemStack stack){return false;} @Override public boolean mayPickup(Player p){return editable();} @Override public boolean isActive(){return visible();} @Override public int getMaxStackSize(){return authority == null ? 16 : authority.transportStorageMaxStackSize();} @Override public int getMaxStackSize(ItemStack stack){return getMaxStackSize();} }
}
