package dev.totem.automata.copper;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/** Non-navigation prerequisites shared by gathering target validation and breaking. */
public final class GatheringTargetPreconditions {
    private GatheringTargetPreconditions() { }
    public static boolean eligible(ServerLevel level, GatheringScanCursor.Bounds bounds, CopperGolemBinding home,
            List<CopperGolemBinding> boundContainers, BlockPos pos) {
        if (!bounds.contains(pos) || home.containerPos().equals(pos) || boundContainers.stream().anyMatch(binding -> binding.dimension().equals(level.dimension()) && binding.containerPos().equals(pos)) || !level.isLoaded(pos)) return false;
        BlockState state = level.getBlockState(pos);
        return !state.isAir() && !state.liquid() && state.getDestroySpeed(level, pos) >= 0 && !GatheringBlockSafety.unsafe(state)
                && !(level.getBlockEntity(pos) instanceof Container);
    }
}
