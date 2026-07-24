package dev.totem.automata.copper;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Blocks that gathering must never select or break. */
public final class GatheringBlockSafety {
    private GatheringBlockSafety() { }
    public static boolean unsafe(BlockState state) {
        return state.is(Blocks.TNT) || state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE) || state.is(Blocks.LAVA)
                || state.is(Blocks.CACTUS) || state.is(Blocks.MAGMA_BLOCK) || state.is(Blocks.CAMPFIRE)
                || state.is(Blocks.SOUL_CAMPFIRE) || state.is(Blocks.RESPAWN_ANCHOR);
    }
}
