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
    public void tick(MinecraftServer server, ServerLevel level, CopperGolem golem, boolean shouldPruneBindings) {
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
            return;
        }
        if (mode != CopperGolemMode.GATHERING) {
            return;
        }
        if (tag.getBooleanOr(CopperGolemData.TAG_TRANSPORT_ENABLED, false)) {
            gathering.tick(server, level, golem, false);
        } else {
            CopperGolemLifecycle.clearGatheringDisplayedItem(golem);
            GatheringLlmWarmup.tick(golem, level);
        }
    }
}
