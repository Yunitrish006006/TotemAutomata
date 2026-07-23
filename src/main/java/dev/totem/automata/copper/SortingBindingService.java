package dev.totem.automata.copper;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

/** Reads and writes Copper Golem sorting sources and destination bindings. */
public final class SortingBindingService {
    public static final String TAG_SOURCE_COPPER_CONTAINER_DIM = "deadrecall_source_copper_container_dim";
    public static final String TAG_SOURCE_COPPER_CONTAINER_X = "deadrecall_source_copper_container_x";
    public static final String TAG_SOURCE_COPPER_CONTAINER_Y = "deadrecall_source_copper_container_y";
    public static final String TAG_SOURCE_COPPER_CONTAINER_Z = "deadrecall_source_copper_container_z";

    private SortingBindingService() {
    }

    public static Optional<CopperGolemBinding> getBinding(CompoundTag tag) {
        List<CopperGolemBinding> bindings = CopperGolemData.readBindings(tag);
        return bindings.isEmpty() ? Optional.empty() : Optional.of(bindings.getFirst());
    }

    public static List<CopperGolemBinding> getBindings(CompoundTag tag) {
        return CopperGolemData.readBindings(tag);
    }

    public static boolean hasBindings(CompoundTag tag) {
        return !getBindings(tag).isEmpty();
    }

    public static Optional<CopperGolemBinding> getSourceContainer(CompoundTag tag) {
        return readSourceContainer(tag);
    }

    public static boolean isSourceContainer(CompoundTag tag, Level level, BlockPos pos) {
        return getSourceContainer(tag)
                .filter(binding -> binding.dimension().equals(level.dimension()) && binding.containerPos().equals(pos))
                .isPresent();
    }

    public static Optional<CopperGolemBinding> readSourceContainer(CompoundTag tag) {
        return CopperGolemData.readBinding(tag,
                TAG_SOURCE_COPPER_CONTAINER_DIM, TAG_SOURCE_COPPER_CONTAINER_X,
                TAG_SOURCE_COPPER_CONTAINER_Y, TAG_SOURCE_COPPER_CONTAINER_Z);
    }

    public static void writeBindings(CompoundTag tag, List<CopperGolemBinding> bindings) {
        CopperGolemData.writeBindings(tag, bindings);
    }

    public static void writeSourceContainer(CompoundTag tag, CopperGolemBinding binding) {
        CopperGolemData.writeBinding(tag, binding,
                TAG_SOURCE_COPPER_CONTAINER_DIM, TAG_SOURCE_COPPER_CONTAINER_X,
                TAG_SOURCE_COPPER_CONTAINER_Y, TAG_SOURCE_COPPER_CONTAINER_Z);
    }

    public static void clearSourceContainer(CompoundTag tag) {
        tag.remove(TAG_SOURCE_COPPER_CONTAINER_DIM);
        tag.remove(TAG_SOURCE_COPPER_CONTAINER_X);
        tag.remove(TAG_SOURCE_COPPER_CONTAINER_Y);
        tag.remove(TAG_SOURCE_COPPER_CONTAINER_Z);
    }
}
