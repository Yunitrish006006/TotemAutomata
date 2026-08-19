package dev.totem.automata.copper;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Module-owned gathering behavior shell. */
public final class PersistedGatheringBehavior implements CopperGolemBehavior {
    private static final String TOOL = "deadrecall_gathering_tool_stack";
    private final WorldOperations world;
    public PersistedGatheringBehavior(WorldOperations world) { this.world = world; }
    @Override public boolean shouldTrack(CopperGolem golem) {
        CompoundTag tag = CopperGolemData.readEntityTag(golem);
        return CopperGolemData.mode(tag) == CopperGolemMode.GATHERING && tag.getBooleanOr(CopperGolemData.TAG_TRANSPORT_ENABLED, false);
    }
    @Override public void tick(MinecraftServer server, ServerLevel level, CopperGolem golem, boolean shouldPruneBindings) {
        CompoundTag tag = CopperGolemData.readEntityTag(golem);
        if (CopperGolemData.migrate(tag)) CopperGolemData.writeEntityTag(golem, tag);
        var bounds = GatheringConfiguration.scanBounds(tag, level.dimension());
        if (bounds.isEmpty()) { GatheringRuntimeState.setActivity(tag, CopperGolemActivity.BLOCKED_NO_AREA); CopperGolemData.writeEntityTag(golem, tag); world.stop(golem); CopperGolemLifecycle.clearGatheringDisplayedItem(golem); return; }
        if (!world.hasHome(golem, level)) { GatheringRuntimeState.setActivity(tag, CopperGolemActivity.BLOCKED_NO_HOME); CopperGolemData.writeEntityTag(golem, tag); world.stop(golem); CopperGolemLifecycle.clearGatheringDisplayedItem(golem); return; }
        List<ItemStack> storage = GatheringStorage.read(tag, level.registryAccess());
        GatheringTickPlan.Action action = GatheringTickPlan.decide(true, true, GatheringStorage.full(storage), CopperGolemData.activity(tag));
        if (!storage.isEmpty() && action == GatheringTickPlan.Action.DEPOSIT) {
            CopperGolemLifecycle.showGatheringDisplayedItem(golem, GatheringStorage.displayStack(storage));
            world.deposit(golem, level, storage);
            return;
        }
        if (!world.hasFuel(golem, level)) { GatheringRuntimeState.setActivity(tag, CopperGolemActivity.BLOCKED_NO_FUEL); CopperGolemData.writeEntityTag(golem, tag); world.stop(golem); CopperGolemLifecycle.clearGatheringDisplayedItem(golem); return; }
        ItemStack tool = CopperGolemData.readItemStack(tag, TOOL, level.registryAccess());
        if (tool.isEmpty()) { GatheringRuntimeState.setActivity(tag, CopperGolemActivity.BLOCKED_NO_TOOL); CopperGolemData.writeEntityTag(golem, tag); world.stop(golem); CopperGolemLifecycle.clearGatheringDisplayedItem(golem); return; }
        if (!world.hasTargetRules(golem, tag)) { GatheringRuntimeState.setActivity(tag, CopperGolemActivity.BLOCKED_NO_VALID_TARGET); CopperGolemData.writeEntityTag(golem, tag); world.stop(golem); CopperGolemLifecycle.clearGatheringDisplayedItem(golem); return; }
        PersistedGatheringScanner.tick(tag, bounds.get(), level.getGameTime(), pos -> world.isValidTarget(golem, level, tag, pos));
        CopperGolemData.writeEntityTag(golem, tag);
        GatheringRuntimeState.target(CopperGolemData.readEntityTag(golem)).ifPresentOrElse(pos -> {
            if (CopperGolemActivityResolver.isAtGatheringTarget(golem, pos)) CopperGolemLifecycle.showGatheringDisplayedItem(golem, tool);
            else CopperGolemLifecycle.clearGatheringDisplayedItem(golem);
            world.tickTarget(golem, level, pos);
        }, () -> CopperGolemLifecycle.clearGatheringDisplayedItem(golem));
    }
    public interface WorldOperations {
        boolean hasHome(CopperGolem golem, ServerLevel level);
        boolean hasFuel(CopperGolem golem, ServerLevel level);
        boolean hasTargetRules(CopperGolem golem, CompoundTag tag);
        boolean isValidTarget(CopperGolem golem, ServerLevel level, CompoundTag tag, BlockPos pos);
        void deposit(CopperGolem golem, ServerLevel level, List<ItemStack> storage);
        void stop(CopperGolem golem);
        void tickTarget(CopperGolem golem, ServerLevel level, BlockPos target);
    }
}
