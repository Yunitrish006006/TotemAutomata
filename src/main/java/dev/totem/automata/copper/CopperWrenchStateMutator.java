package dev.totem.automata.copper;

import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.List;

/** Legacy-compatible persisted mutations performed by Wrench binding gestures. */
public final class CopperWrenchStateMutator {
    private CopperWrenchStateMutator() { }

    public static boolean addBinding(CompoundTag tag, CopperGolemBinding binding) {
        List<CopperGolemBinding> bindings = new ArrayList<>(SortingBindingService.getBindings(tag));
        if (bindings.contains(binding)) return false;
        bindings.add(binding); writeBindingsAndReset(tag, bindings); return true;
    }
    public static boolean removeBinding(CompoundTag tag, CopperGolemBinding binding) {
        List<CopperGolemBinding> bindings = new ArrayList<>(SortingBindingService.getBindings(tag));
        if (!bindings.remove(binding)) return false;
        writeBindingsAndReset(tag, bindings); return true;
    }
    public static boolean setSource(CompoundTag tag, CopperGolemBinding binding) {
        if (SortingBindingService.getSourceContainer(tag).filter(binding::equals).isPresent()) return false;
        SortingBindingService.writeSourceContainer(tag, binding); GatheringRuntimeState.resetSearch(tag, true); CopperGolemData.bumpRevision(tag);
        List<CopperGolemBinding> bindings = new ArrayList<>(SortingBindingService.getBindings(tag));
        if (bindings.remove(binding)) writeBindingsAndReset(tag, bindings);
        return true;
    }
    public static boolean removeSource(CompoundTag tag, CopperGolemBinding binding) {
        if (SortingBindingService.getSourceContainer(tag).filter(binding::equals).isEmpty()) return false;
        SortingBindingService.clearSourceContainer(tag); GatheringRuntimeState.resetSearch(tag, true); CopperGolemData.bumpRevision(tag); return true;
    }
    public static GatheringConfiguration.CornerUpdate setGatheringCorner(CompoundTag tag, net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension,
            net.minecraft.core.BlockPos pos, boolean cornerB) {
        GatheringConfiguration.CornerUpdate update = GatheringConfiguration.setCorner(tag, dimension, pos, cornerB);
        if (update == GatheringConfiguration.CornerUpdate.UPDATED) { GatheringRuntimeState.resetSearch(tag, true); CopperGolemData.bumpRevision(tag); }
        return update;
    }
    public static boolean toggleGatheringTarget(CompoundTag tag, String blockId) {
        boolean added = GatheringConfiguration.toggleManualTarget(tag, blockId);
        GatheringRuntimeState.resetSearch(tag, true); CopperGolemData.bumpRevision(tag); return added;
    }
    private static void writeBindingsAndReset(CompoundTag tag, List<CopperGolemBinding> bindings) {
        SortingBindingService.writeBindings(tag, bindings);
        SortingLlmState.write(tag, SortingLlmState.read(tag).stream()
                .filter(config -> bindings.contains(config.binding()))
                .toList());
        CopperGolemStateMutation.clearSortingBlocked(tag);
        CopperGolemData.bumpRevision(tag);
    }
}
