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
        tick(server, level, golem, shouldPruneBindings, GatheringScanCursor.DEFAULT_BUDGET);
    }
    public void tick(MinecraftServer server, ServerLevel level, CopperGolem golem, boolean shouldPruneBindings, int scanBudget) {
        tickScheduled(server, level, golem, shouldPruneBindings, scanBudget);
    }
    @Override public TickResult tickScheduled(MinecraftServer server, ServerLevel level, CopperGolem golem,
                                              boolean shouldPruneBindings, int scanBudget) {
        CompoundTag tag = CopperGolemData.readEntityTag(golem);
        if (CopperGolemData.migrate(tag)) CopperGolemData.writeEntityTag(golem, tag);
        var bounds = GatheringConfiguration.scanBounds(tag, level.dimension());
        if (bounds.isEmpty()) { block(golem, tag, CopperGolemActivity.BLOCKED_NO_AREA); return TickResult.NONE; }
        if (!world.hasHome(golem, level, tag)) { block(golem, tag, CopperGolemActivity.BLOCKED_NO_HOME); return TickResult.NONE; }
        List<ItemStack> storage = GatheringStorage.read(tag, level.registryAccess());
        GatheringTickPlan.Action action = GatheringTickPlan.decide(true, true, GatheringStorage.full(storage), CopperGolemData.activity(tag));
        if (!storage.isEmpty() && action == GatheringTickPlan.Action.DEPOSIT) {
            CopperGolemLifecycle.showGatheringDisplayedItem(golem, GatheringStorage.displayStack(storage));
            world.deposit(golem, level, storage);
            return TickResult.NONE;
        }
        if (!world.hasFuel(golem, level, tag)) { block(golem, tag, CopperGolemActivity.BLOCKED_NO_FUEL); return TickResult.NONE; }
        ItemStack tool = CopperGolemData.readItemStack(tag, TOOL, level.registryAccess());
        if (tool.isEmpty()) { block(golem, tag, CopperGolemActivity.BLOCKED_NO_TOOL); return TickResult.NONE; }
        if (!world.hasTargetRules(golem, tag)) { block(golem, tag, CopperGolemActivity.BLOCKED_NO_VALID_TARGET); return TickResult.NONE; }
        var currentTarget = GatheringRuntimeState.target(tag);
        if (currentTarget.isPresent()) {
            BlockPos target = currentTarget.orElseThrow();
            if (!world.isCheaplyValidTarget(golem, level, tag, bounds.get(), target)) {
                GatheringRuntimeState.clearTarget(tag);
                GatheringBreakProgress.clear(tag);
                CopperGolemData.writeEntityTag(golem, tag);
                GatheringNavigation.forget(golem);
                level.destroyBlockProgress(golem.getId(), target, -1);
                CopperGolemLifecycle.clearGatheringDisplayedItem(golem);
                return TickResult.NONE;
            }
            if (CopperGolemActivityResolver.isAtGatheringTarget(golem, target)) CopperGolemLifecycle.showGatheringDisplayedItem(golem, tool);
            else CopperGolemLifecycle.clearGatheringDisplayedItem(golem);
            world.tickTarget(golem, level, tag, target);
            return TickResult.NONE;
        }
        if (scanBudget <= 0) return TickResult.NONE;
        CompoundTag beforeScan = tag.copy();
        GatheringScanCursor.Step scanStep = PersistedGatheringScanner.tick(
                tag,
                bounds.get(),
                level.getGameTime(),
                scanBudget,
                pos -> world.isCheaplyValidTarget(golem, level, tag, bounds.get(), pos),
                pos -> world.isValidTarget(golem, level, tag, pos)
        );
        if (!tag.equals(beforeScan)) CopperGolemData.writeEntityTag(golem, tag);
        GatheringRuntimeState.target(tag).ifPresentOrElse(pos -> {
            if (CopperGolemActivityResolver.isAtGatheringTarget(golem, pos)) CopperGolemLifecycle.showGatheringDisplayedItem(golem, tool);
            else CopperGolemLifecycle.clearGatheringDisplayedItem(golem);
            world.tickTarget(golem, level, tag, pos);
        }, () -> CopperGolemLifecycle.clearGatheringDisplayedItem(golem));
        return new TickResult(scanStep.inspectedPositions());
    }

    private void block(CopperGolem golem, CompoundTag tag, CopperGolemActivity activity) {
        if (!GatheringRuntimeState.setActivity(tag, activity)) return;
        CopperGolemData.writeEntityTag(golem, tag);
        world.stop(golem);
        GatheringNavigation.forget(golem);
        CopperGolemLifecycle.clearGatheringDisplayedItem(golem);
    }

    public void stop(CopperGolem golem, CompoundTag tag) {
        world.stop(golem);
        GatheringNavigation.forget(golem);
        GatheringRuntimeState.setActivity(tag, CopperGolemActivity.STOPPED);
        CopperGolemData.writeEntityTag(golem, tag);
        CopperGolemLifecycle.clearGatheringDisplayedItem(golem);
    }
    public interface WorldOperations {
        boolean hasHome(CopperGolem golem, ServerLevel level, CompoundTag tag);
        boolean hasFuel(CopperGolem golem, ServerLevel level, CompoundTag tag);
        boolean hasTargetRules(CopperGolem golem, CompoundTag tag);
        boolean isCheaplyValidTarget(CopperGolem golem, ServerLevel level, CompoundTag tag,
                                     GatheringScanCursor.Bounds bounds, BlockPos pos);
        boolean isValidTarget(CopperGolem golem, ServerLevel level, CompoundTag tag, BlockPos pos);
        void deposit(CopperGolem golem, ServerLevel level, List<ItemStack> storage);
        void stop(CopperGolem golem);
        void tickTarget(CopperGolem golem, ServerLevel level, CompoundTag tag, BlockPos target);
    }
}
