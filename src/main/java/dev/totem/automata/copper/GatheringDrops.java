package dev.totem.automata.copper;

import dev.totem.automata.excavation.TotemExcavationHammerAdapter;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

/** Safe, bounded block-drop resolution for gathering target validation and break execution. */
public final class GatheringDrops {
    private GatheringDrops() { }
    public static Optional<List<ItemStack>> resolve(CopperGolem golem, ServerLevel level, BlockPos pos, BlockState state, ItemStack tool) {
        if (tool.isEmpty()
                || TotemExcavationHammerAdapter.isSupported(tool) && !tool.isCorrectToolForDrops(state)
                || state.requiresCorrectToolForDrops() && !tool.isCorrectToolForDrops(state)) {
            return Optional.empty();
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        return GatheringStorage.normalizeDrops(Block.getDrops(state, level, pos, blockEntity, golem, tool));
    }
}
