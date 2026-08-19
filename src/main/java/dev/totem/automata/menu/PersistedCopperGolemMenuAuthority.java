package dev.totem.automata.menu;

import dev.totem.automata.copper.CopperGolemData;
import dev.totem.automata.copper.CopperGolemFuelService;
import dev.totem.automata.copper.CopperWrenchSelection;
import dev.totem.automata.copper.GatheringStorage;
import dev.totem.automata.copper.GatheringToolPolicy;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.BiConsumer;

/** Menu authority backed by the migrated Copper Golem NBT schema. */
public final class PersistedCopperGolemMenuAuthority implements CopperGolemMenuAuthority {
    private static final String TOOL = "deadrecall_gathering_tool_stack";
    private final BiConsumer<ServerPlayer, CopperGolem> refresher;
    public PersistedCopperGolemMenuAuthority(BiConsumer<ServerPlayer, CopperGolem> refresher) { this.refresher = refresher; }
    @Override public boolean canUse(Player player, CopperGolem golem) {
        if (golem.isRemoved() || !golem.isAlive() || !golem.level().dimension().equals(player.level().dimension()) || player.distanceToSqr(golem) > 4096D) return false;
        if (!(player instanceof ServerPlayer server)) return true;
        return golem.getUUID().equals(CopperWrenchSelection.selectedGolem(server.getMainHandItem()))
                || golem.getUUID().equals(CopperWrenchSelection.selectedGolem(server.getOffhandItem()));
    }
    @Override public boolean isFuel(ServerLevel level, ItemStack stack) { return CopperGolemFuelService.isFuel(level, stack); }
    @Override public boolean canEditGatheringSlots(CopperGolem golem) { CompoundTag tag = tag(golem); return "gathering".equals(tag.getStringOr(CopperGolemData.TAG_MODE, "sorting")) && !tag.getBooleanOr(CopperGolemData.TAG_TRANSPORT_ENABLED, false); }
    @Override public boolean isGatheringMode(CopperGolem golem) { return "gathering".equals(tag(golem).getStringOr(CopperGolemData.TAG_MODE, "sorting")); }
    @Override public boolean isGatheringTool(ItemStack stack) { return GatheringToolPolicy.accepts(stack); }
    @Override public int transportStorageMaxItemCount() { return GatheringStorage.MAX_STACK_SIZE; }
    @Override public ItemStack fuel(CopperGolem golem) { return CopperGolemData.readItemStack(tag(golem), CopperGolemData.TAG_FUEL_STACK, golem.level().registryAccess()); }
    @Override public ItemStack gatheringTool(CopperGolem golem) { return CopperGolemData.readItemStack(tag(golem), TOOL, golem.level().registryAccess()); }
    @Override public List<ItemStack> gatheringStorage(CopperGolem golem) { return GatheringStorage.read(tag(golem), golem.level().registryAccess()); }
    @Override public void setFuel(CopperGolem golem, ItemStack stack) { CompoundTag tag = tag(golem); CopperGolemData.writeItemStack(tag, CopperGolemData.TAG_FUEL_STACK, stack, golem.level().registryAccess()); write(golem, tag, false); }
    @Override public void setGatheringTool(CopperGolem golem, ItemStack stack) { CompoundTag tag = tag(golem); CopperGolemData.writeItemStack(tag, TOOL, stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1), golem.level().registryAccess()); write(golem, tag, true); }
    @Override public void setGatheringStorage(CopperGolem golem, List<ItemStack> stacks) { CompoundTag tag = tag(golem); GatheringStorage.write(tag, stacks, golem.level().registryAccess()); write(golem, tag, true); }
    @Override public void refresh(ServerPlayer viewer, CopperGolem golem) { refresher.accept(viewer, golem); }
    private static CompoundTag tag(CopperGolem golem) { return CopperGolemData.readEntityTag(golem); }
    private static void write(CopperGolem golem, CompoundTag tag, boolean revision) { if (revision) CopperGolemData.bumpRevision(tag); CopperGolemData.writeEntityTag(golem, tag); }
}
