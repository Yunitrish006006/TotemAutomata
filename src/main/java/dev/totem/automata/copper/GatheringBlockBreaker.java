package dev.totem.automata.copper;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Event-gated gathering break that commits drops into the golem's carried storage. */
public final class GatheringBlockBreaker {
    private static final String STORAGE = "deadrecall_gathering_storage_stack", TOOL = "deadrecall_gathering_tool_stack";
    private GatheringBlockBreaker() { }
    public static Result breakTarget(CopperGolem golem, ServerLevel level, ServerPlayer operator, BlockPos pos) {
        CompoundTag current = CopperGolemData.readEntityTag(golem); BlockState state = level.getBlockState(pos); ItemStack tool = CopperGolemData.readItemStack(current, TOOL, level.registryAccess());
        var drops = GatheringDrops.resolve(golem, level, pos, state, tool); ItemStack storage = CopperGolemData.readItemStack(current, STORAGE, level.registryAccess());
        if (drops.isEmpty() || !GatheringStorage.canStore(storage, drops.get()) || !GatheringBreakPermission.allowed(operator, level, pos, state, tool)) return Result.REJECTED;
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!GatheringBreakEvents.before(level, operator, pos, state, blockEntity)) return Result.REJECTED;
        var transaction = GatheringBreakTransaction.prepare(current, level, storage, tool, drops.get()); if (transaction.isEmpty()) return Result.REJECTED;
        if (!level.destroyBlock(pos, false, golem)) return Result.REJECTED;
        CopperGolemData.writeEntityTag(golem, transaction.get().tag());
        PlayerBlockBreakEvents.AFTER.invoker().afterBlockBreak(level, operator, pos, state, blockEntity);
        level.levelEvent(2001, pos, Block.getId(state)); level.sendParticles(ParticleTypes.WAX_ON, pos.getX()+.5D, pos.getY()+.5D, pos.getZ()+.5D, 6, .2D, .2D, .2D, .02D);
        golem.swing(InteractionHand.MAIN_HAND, true);
        if (transaction.get().toolBroken()) { CompoundTag tag = CopperGolemData.readEntityTag(golem); GatheringRuntimeState.setActivity(tag, CopperGolemActivity.BLOCKED_TOOL_BROKEN); CopperGolemData.writeEntityTag(golem, tag); return Result.TOOL_BROKEN; }
        return Result.BROKEN;
    }
    public enum Result { BROKEN, TOOL_BROKEN, REJECTED }
}
