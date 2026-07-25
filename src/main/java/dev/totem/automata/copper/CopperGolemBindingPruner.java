package dev.totem.automata.copper;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ai.behavior.TransportItemsBetweenContainers;
import net.minecraft.world.entity.animal.golem.CopperGolem;

import java.util.List;

/** Prunes only loaded, no-longer-valid persisted source/destination bindings. */
public final class CopperGolemBindingPruner {
    private CopperGolemBindingPruner() {
    }

    /** Returns whether persisted state changed. Unloaded dimensions/chunks are intentionally retained. */
    public static boolean prune(CopperGolem golem, MinecraftServer server) {
        CompoundTag tag = CopperGolemData.readEntityTag(golem);
        boolean changed = false;

        var source = SortingBindingService.getSourceContainer(tag);
        if (source.isPresent() && loaded(server, source.get()) && !sourceAvailable(server, source.get())) {
            SortingBindingService.clearSourceContainer(tag);
            GatheringRuntimeState.resetSearch(tag, true);
            CopperGolemStateMutation.clearSortingBlocked(tag);
            CopperGolemData.bumpRevision(tag);
            changed = true;
        }

        List<CopperGolemBinding> bindings = SortingBindingService.getBindings(tag);
        List<CopperGolemBinding> kept = bindings.stream()
                .filter(binding -> !loaded(server, binding) || destinationAvailable(server, binding))
                .toList();
        if (kept.size() != bindings.size()) {
            SortingBindingService.writeBindings(tag, kept);
            SortingLlmState.write(tag, SortingLlmState.read(tag).stream()
                    .filter(config -> kept.contains(config.binding()))
                    .toList());
            CopperGolemStateMutation.clearSortingBlocked(tag);
            CopperGolemData.bumpRevision(tag);
            changed = true;
        }

        if (changed) {
            CopperGolemData.writeEntityTag(golem, tag);
        }
        return changed;
    }

    private static boolean loaded(MinecraftServer server, CopperGolemBinding binding) {
        ServerLevel level = server.getLevel(binding.dimension());
        return level != null && level.isLoaded(binding.containerPos());
    }

    private static boolean sourceAvailable(MinecraftServer server, CopperGolemBinding binding) {
        ServerLevel level = server.getLevel(binding.dimension());
        BlockPos pos = binding.containerPos();
        return level != null && level.getBlockState(pos).is(BlockTags.COPPER_CHESTS)
                && level.getBlockEntity(pos) instanceof Container;
    }

    private static boolean destinationAvailable(MinecraftServer server, CopperGolemBinding binding) {
        ServerLevel level = server.getLevel(binding.dimension());
        if (level == null || level.getBlockState(binding.containerPos()).is(BlockTags.COPPER_CHESTS)) {
            return false;
        }
        return TransportItemsBetweenContainers.TransportItemTarget.tryCreatePossibleTarget(binding.containerPos(), level) != null;
    }
}
