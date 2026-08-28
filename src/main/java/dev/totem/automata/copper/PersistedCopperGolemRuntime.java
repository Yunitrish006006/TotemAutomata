package dev.totem.automata.copper;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.golem.CopperGolem;

/**
 * Complete external server tick seam for Copper Golem persisted state.
 *
 * <p>Normal sorting transport remains supplied by the matching external
 * mixin, while this runtime owns discovery, pruning, blocked sorting, and the
 * entire gathering path. It becomes live only through the atomic cutover
 * composition.</p>
 */
public final class PersistedCopperGolemRuntime implements CopperGolemBehavior {
    private static final String SORTING_BLOCKED = "deadrecall_sorting_blocked";

    private final SortingOperations sorting = new PersistedSortingOperations(new DefaultItemMetadata());
    private final PersistedGatheringBehavior gathering = new PersistedGatheringBehavior(new DefaultGatheringWorldOperations());

    @Override
    public boolean shouldTrack(CopperGolem golem) {
        if (golem.isRemoved() || !golem.isAlive()) {
            return false;
        }
        CompoundTag tag = CopperGolemData.readEntityTag(golem);
        return CopperGolemData.mode(tag) == CopperGolemMode.GATHERING
                || tag.getBooleanOr(SORTING_BLOCKED, false)
                || !SortingBindingService.getBindings(tag).isEmpty()
                || SortingBindingService.getSourceContainer(tag).isPresent();
    }

    @Override
    public boolean needsScanBudget(CopperGolem golem) {
        CompoundTag tag = CopperGolemData.readEntityTag(golem);
        return CopperGolemData.mode(tag) == CopperGolemMode.GATHERING
                && tag.getBooleanOr(CopperGolemData.TAG_TRANSPORT_ENABLED, false)
                && GatheringRuntimeState.target(tag).isEmpty();
    }

    @Override
    public Scheduling scheduling(ServerLevel level, CopperGolem golem) {
        if (golem.isRemoved() || !golem.isAlive()) return new Scheduling(false, false);
        CompoundTag tag = CopperGolemData.readEntityTag(golem);
        CopperGolemMode mode = CopperGolemData.mode(tag);
        boolean shouldTick = mode == CopperGolemMode.GATHERING
                || tag.getBooleanOr(SORTING_BLOCKED, false)
                || !SortingBindingService.getBindings(tag).isEmpty()
                || SortingBindingService.getSourceContainer(tag).isPresent();
        if (!shouldTick || mode != CopperGolemMode.GATHERING
                || !tag.getBooleanOr(CopperGolemData.TAG_TRANSPORT_ENABLED, false)
                || GatheringRuntimeState.target(tag).isPresent()) {
            return new Scheduling(shouldTick, false);
        }
        boolean waiting = CopperGolemData.activity(tag) == CopperGolemActivity.BLOCKED_NO_VALID_TARGET
                && level.getGameTime() < GatheringRuntimeState.retryTick(tag);
        boolean prerequisites = !waiting
                && GatheringConfiguration.scanBounds(tag, level.dimension()).isPresent()
                && GatheringHomeResolver.resolve(tag, level).isPresent()
                && CopperGolemFuelService.hasFuelAvailable(tag, level)
                && !CopperGolemData.readItemStack(tag, "deadrecall_gathering_tool_stack", level.registryAccess()).isEmpty()
                && GatheringTargetPolicy.hasRules(
                        GatheringConfiguration.manualTargets(tag),
                        GatheringLlmState.read(tag),
                        GolemLlmState.read(tag)
                );
        if (prerequisites) {
            var storage = GatheringStorage.read(tag, level.registryAccess());
            CopperGolemActivity activity = CopperGolemData.activity(tag);
            GatheringTickPlan.Action action = GatheringTickPlan.decide(
                    true,
                    true,
                    GatheringStorage.full(storage),
                    activity
            );
            prerequisites = storage.isEmpty() || action != GatheringTickPlan.Action.DEPOSIT;
        }
        return new Scheduling(true, prerequisites);
    }

    @Override
    public void tick(MinecraftServer server, ServerLevel level, CopperGolem golem, boolean shouldPruneBindings) {
        tick(server, level, golem, shouldPruneBindings, GatheringScanCursor.DEFAULT_BUDGET);
    }

    @Override
    public void tick(MinecraftServer server, ServerLevel level, CopperGolem golem,
                     boolean shouldPruneBindings, int scanBudget) {
        tickScheduled(server, level, golem, shouldPruneBindings, scanBudget);
    }

    @Override
    public TickResult tickScheduled(MinecraftServer server, ServerLevel level, CopperGolem golem,
                                    boolean shouldPruneBindings, int scanBudget) {
        CompoundTag tag = CopperGolemData.readEntityTag(golem);
        if (CopperGolemData.migrate(tag)) {
            CopperGolemData.writeEntityTag(golem, tag);
        }
        if (shouldPruneBindings) {
            CopperGolemBindingPruner.prune(golem, server);
            tag = CopperGolemData.readEntityTag(golem);
        }

        CopperGolemMode mode = CopperGolemData.mode(tag);
        if (mode == CopperGolemMode.SORTING
                && tag.getBooleanOr(CopperGolemData.TAG_TRANSPORT_ENABLED, false)
                && tag.getBooleanOr(SORTING_BLOCKED, false)) {
            SortingModeController.tickBlocked(golem, level, sorting);
            return TickResult.NONE;
        }
        if (mode != CopperGolemMode.GATHERING) {
            return TickResult.NONE;
        }
        if (tag.getBooleanOr(CopperGolemData.TAG_TRANSPORT_ENABLED, false)) {
            return gathering.tickScheduled(server, level, golem, false, scanBudget);
        } else {
            if (CopperGolemData.activity(tag) != CopperGolemActivity.STOPPED) {
                gathering.stop(golem, tag);
            }
            return TickResult.NONE;
        }
    }
}
