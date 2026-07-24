package dev.totem.automata.copper;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Live Wrench callback authority to be supplied by the final Automata runtime adapter. */
public interface CopperWrenchInteractionAuthority {
    InteractionResult attackBlock(Player player, Level level, InteractionHand hand, BlockPos pos);
    InteractionResult useBlock(Player player, Level level, InteractionHand hand, BlockPos pos);
    InteractionResult useEntity(Player player, Level level, InteractionHand hand, Entity entity);
}
