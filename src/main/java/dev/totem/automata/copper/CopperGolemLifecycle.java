package dev.totem.automata.copper;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

/**
 * External lifecycle cleanup for the persisted Copper Golem inventory.
 *
 * <p>It is intentionally event-framework agnostic. The final Automata
 * bootstrap registers it for the death/destruction lifecycle only after the
 * matching DeadRecall callbacks have been gated off.</p>
 */
public final class CopperGolemLifecycle {
    private static final String GATHERING_TOOL = "deadrecall_gathering_tool_stack";
    private static final String GATHERING_STORAGE = "deadrecall_gathering_storage_stack";

    private CopperGolemLifecycle() {
    }

    /** Clears the virtual gathering tool/storage display without touching sorting cargo. */
    public static void clearGatheringDisplayedItem(CopperGolem golem) {
        CompoundTag tag = CopperGolemData.readEntityTag(golem);
        if (CopperGolemData.mode(tag) == CopperGolemMode.GATHERING && !golem.getMainHandItem().isEmpty()) {
            golem.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        }
    }

    /** Shows a bounded virtual gathering item without changing persisted cargo. */
    public static void showGatheringDisplayedItem(CopperGolem golem, ItemStack stack) {
        ItemStack display = stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
        if (!display.isEmpty()) {
            display.setCount(Math.min(display.getCount(), GatheringStorage.MAX_STACK_SIZE));
        }
        if (!ItemStack.isSameItemSameComponents(golem.getMainHandItem(), display)
                || golem.getMainHandItem().getCount() != display.getCount()) {
            golem.setItemInHand(InteractionHand.MAIN_HAND, display);
        }
    }

    /**
     * Drops fuel, gathering tool and gathered storage exactly once, then
     * clears their persisted copies before the entity disappears.
     */
    public static void dropGatheringInventory(CopperGolem golem) {
        if (!(golem.level() instanceof ServerLevel level)) {
            return;
        }
        clearGatheringDisplayedItem(golem);

        CompoundTag tag = CopperGolemData.readEntityTag(golem);
        ItemStack fuel = CopperGolemFuelService.readFuelStack(tag, level);
        ItemStack tool = CopperGolemData.readItemStack(tag, GATHERING_TOOL, level.registryAccess());
        ItemStack storage = CopperGolemData.readItemStack(tag, GATHERING_STORAGE, level.registryAccess());
        if (storage.getCount() > GatheringStorage.MAX_STACK_SIZE) {
            storage = storage.copyWithCount(GatheringStorage.MAX_STACK_SIZE);
        }
        if (fuel.isEmpty() && tool.isEmpty() && storage.isEmpty()) {
            return;
        }

        CopperGolemFuelService.writeFuelStack(tag, ItemStack.EMPTY, level);
        tag.remove(CopperGolemData.TAG_FUEL_TICKS);
        CopperGolemStateMutation.clearSortingBlocked(tag);
        CopperGolemData.bumpRevision(tag);
        CopperGolemData.writeItemStack(tag, GATHERING_TOOL, ItemStack.EMPTY, level.registryAccess());
        GatheringRuntimeState.resetSearch(tag, true);
        CopperGolemData.writeItemStack(tag, GATHERING_STORAGE, ItemStack.EMPTY, level.registryAccess());
        GatheringRuntimeState.resetSearch(tag, false);
        CopperGolemData.writeEntityTag(golem, tag);

        drop(level, golem, fuel);
        drop(level, golem, tool);
        drop(level, golem, storage);
    }

    private static void drop(ServerLevel level, CopperGolem golem, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        ItemEntity item = new ItemEntity(level, golem.getX(), golem.getY() + 0.25D, golem.getZ(), stack.copy());
        item.setDeltaMovement(
                (golem.getRandom().nextDouble() - 0.5D) * 0.12D,
                0.18D,
                (golem.getRandom().nextDouble() - 0.5D) * 0.12D);
        item.setDefaultPickUpDelay();
        level.addFreshEntity(item);
    }
}
