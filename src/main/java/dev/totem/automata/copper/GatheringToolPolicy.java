package dev.totem.automata.copper;

import dev.totem.automata.excavation.TotemExcavationHammerAdapter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Server and menu policy for items that may occupy the Copper Golem tool slot. */
public final class GatheringToolPolicy {
    private static final BlockState[] PROBE_STATES = {
            Blocks.STONE.defaultBlockState(),
            Blocks.DIRT.defaultBlockState(),
            Blocks.OAK_LOG.defaultBlockState(),
            Blocks.SAND.defaultBlockState(),
            Blocks.GRAVEL.defaultBlockState(),
            Blocks.COBWEB.defaultBlockState()
    };

    private GatheringToolPolicy() {
    }

    public static boolean accepts(ItemStack stack) {
        if (stack.isEmpty() || CopperWrenchSelection.isCopperWrench(stack)) {
            return false;
        }
        if (TotemExcavationHammerAdapter.isSupported(stack) || stack.isDamageableItem()) {
            return true;
        }
        for (BlockState state : PROBE_STATES) {
            if (stack.getDestroySpeed(state) > 1.0F || stack.isCorrectToolForDrops(state)) {
                return true;
            }
        }
        return false;
    }
}
