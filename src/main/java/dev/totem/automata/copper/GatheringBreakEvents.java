package dev.totem.automata.copper;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Fabric break-event gate retained by gathering's final block-break transaction. */
public final class GatheringBreakEvents {
    private GatheringBreakEvents() { }
    public static boolean before(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state, BlockEntity blockEntity) {
        if (PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(level, player, pos, state, blockEntity)) return true;
        PlayerBlockBreakEvents.CANCELED.invoker().onBlockBreakCanceled(level, player, pos, state, blockEntity);
        return false;
    }
}
