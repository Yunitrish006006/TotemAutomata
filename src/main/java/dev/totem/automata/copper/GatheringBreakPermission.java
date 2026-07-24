package dev.totem.automata.copper;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.GameMasterBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Operator-based protection and tool authorization for a gathering break. */
public final class GatheringBreakPermission {
    private GatheringBreakPermission() { }
    public static boolean allowed(ServerPlayer player, ServerLevel level, BlockPos pos, BlockState state, ItemStack tool) {
        return player.level() == level && !level.getServer().isUnderSpawnProtection(level, pos, player)
                && level.mayInteract(player, pos)
                && (!(state.getBlock() instanceof GameMasterBlock) || player.canUseGameMasterBlocks())
                && !player.blockActionRestricted(level, pos, player.gameMode.getGameModeForPlayer())
                && tool.canDestroyBlock(state, level, pos, player);
    }
}
