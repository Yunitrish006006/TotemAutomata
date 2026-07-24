package dev.totem.automata.copper;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.item.ItemStack;

/** Concrete module-owned prerequisite, scanner, and deposit operations for gathering. */
public final class DefaultGatheringWorldOperations implements PersistedGatheringBehavior.WorldOperations {
    private static final String TOOL="deadrecall_gathering_tool_stack", STORAGE="deadrecall_gathering_storage_stack";
    @Override public boolean hasHome(CopperGolem golem, ServerLevel level) { return GatheringHomeResolver.resolve(CopperGolemData.readEntityTag(golem), level).isPresent(); }
    @Override public boolean hasFuel(CopperGolem golem, ServerLevel level) { return CopperGolemFuelService.hasFuelAvailable(CopperGolemData.readEntityTag(golem), level); }
    @Override public boolean hasTargetRules(CopperGolem golem, CompoundTag tag) { return GatheringTargetPolicy.hasRules(GatheringConfiguration.manualTargets(tag), GatheringLlmState.read(tag), GolemLlmState.read(tag)); }
    @Override public boolean isValidTarget(CopperGolem golem, ServerLevel level, CompoundTag tag, BlockPos pos) {
        var home=GatheringHomeResolver.resolve(tag,level); var bounds=GatheringConfiguration.scanBounds(tag,level.dimension()); if(home.isEmpty()||bounds.isEmpty())return false;
        if(!GatheringTargetPreconditions.eligible(level,bounds.get(),home.get().binding(),CopperGolemData.readBindings(tag),pos)||GatheringNavigation.destinations(level,golem,pos).isEmpty())return false;
        ItemStack tool=CopperGolemData.readItemStack(tag,TOOL); var state=level.getBlockState(pos); var drops=GatheringDrops.resolve(golem,level,pos,state,tool); if(drops.isEmpty()||!GatheringStorage.canStore(CopperGolemData.readItemStack(tag,STORAGE),drops.get()))return false;
        String id=net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        return GatheringTargetPolicy.decide(id,state.getBlock().builtInRegistryHolder().tags().map(key->key.location().toString()).sorted().toList(),GatheringConfiguration.manualTargets(tag),GatheringLlmState.read(tag),GolemLlmState.read(tag)).allowed();
    }
    @Override public void deposit(CopperGolem golem, ServerLevel level, ItemStack storage) { GatheringHomeResolver.resolve(CopperGolemData.readEntityTag(golem),level).ifPresent(home->GatheringHomeDeposit.tick(golem,level,home.binding().containerPos(),home.container(),storage)); }
    @Override public void stop(CopperGolem golem) { golem.getNavigation().stop(); }
    @Override public void tickTarget(CopperGolem golem, ServerLevel level, BlockPos target) {
        if (golem.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(target)) > 4D) { CompoundTag moving=CopperGolemData.readEntityTag(golem); GatheringRuntimeState.setActivity(moving, CopperGolemActivity.MOVING_TO_TARGET); CopperGolemData.writeEntityTag(golem,moving); GatheringNavigation.moveOrSkip(golem, level, target); return; }
        CompoundTag tag=CopperGolemData.readEntityTag(golem); var state=level.getBlockState(target); ItemStack tool=CopperGolemData.readItemStack(tag,TOOL); var progress=GatheringBreakProgress.advance(tag,level,target,state,tool); CopperGolemData.writeEntityTag(golem,tag); level.destroyBlockProgress(golem.getId(),target,progress.crackStage()); if(progress.progressTicks()%5==0){level.levelEvent(2001,target,net.minecraft.world.level.block.Block.getId(state));golem.swing(net.minecraft.world.InteractionHand.MAIN_HAND,true);} if(progress.complete()){level.destroyBlockProgress(golem.getId(),target,-1);GatheringOperator.resolve(golem,level).ifPresent(player->GatheringBlockBreaker.breakTarget(golem,level,player,target));}
    }
}
