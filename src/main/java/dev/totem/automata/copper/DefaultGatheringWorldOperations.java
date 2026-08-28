package dev.totem.automata.copper;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Concrete module-owned prerequisite, scanner, and deposit operations for gathering. */
public final class DefaultGatheringWorldOperations implements PersistedGatheringBehavior.WorldOperations {
    private static final String TOOL="deadrecall_gathering_tool_stack";
    @Override public boolean hasHome(CopperGolem golem, ServerLevel level, CompoundTag tag) { return GatheringHomeResolver.resolve(tag, level).isPresent(); }
    @Override public boolean hasFuel(CopperGolem golem, ServerLevel level, CompoundTag tag) { return CopperGolemFuelService.hasFuelAvailable(tag, level); }
    @Override public boolean hasTargetRules(CopperGolem golem, CompoundTag tag) { return GatheringTargetPolicy.hasRules(GatheringConfiguration.manualTargets(tag), GatheringLlmState.read(tag), GolemLlmState.read(tag)); }
    @Override public boolean isCheaplyValidTarget(CopperGolem golem, ServerLevel level, CompoundTag tag,
                                                   GatheringScanCursor.Bounds bounds, BlockPos pos) {
        var home = GatheringHomeResolver.resolve(tag, level);
        return home.isPresent() && GatheringTargetPreconditions.eligible(
                level, bounds, home.get().binding(), CopperGolemData.readBindings(tag), pos);
    }
    @Override public boolean isValidTarget(CopperGolem golem, ServerLevel level, CompoundTag tag, BlockPos pos) {
        var home=GatheringHomeResolver.resolve(tag,level); var bounds=GatheringConfiguration.scanBounds(tag,level.dimension()); if(home.isEmpty()||bounds.isEmpty())return false;
        if(!GatheringTargetPreconditions.eligible(level,bounds.get(),home.get().binding(),CopperGolemData.readBindings(tag),pos)||GatheringNavigation.destinations(level,golem,pos).isEmpty())return false;
        ItemStack tool=CopperGolemData.readItemStack(tag,TOOL,level.registryAccess());
        var state=level.getBlockState(pos);
        var drops=GatheringDrops.resolve(golem,level,pos,state,tool);
        if(drops.isEmpty()||!GatheringStorage.canStore(GatheringStorage.read(tag,level.registryAccess()),drops.get()))return false;
        var operator=GatheringOperator.resolve(golem,level); if(operator.isEmpty()||!GatheringBreakPermission.allowed(operator.get(),level,pos,state,tool))return false;
        String id=net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        List<String> blockTags=GatheringLlmPromptData.blockTags(state);
        GatheringLlmState.Config llm=GatheringLlmState.read(tag); GolemLlmState.Config golemLlm=GolemLlmState.read(tag);
        GatheringTargetPolicy.Decision decision=GatheringTargetPolicy.decide(id,blockTags,GatheringConfiguration.manualTargets(tag),llm,golemLlm);
        if(decision.requestsClassification()) BlockLlmClassifier.requestClassification(level.getServer(),golem.getUUID(),id,state.getBlock().getName().getString(),blockTags,
                GatheringLlmPromptData.dropSummary(drops.get()),GatheringLlmPromptData.toolSummary(tool),llm.prompt(),llm.promptRevision(),golemLlm.apiUrl(),golemLlm.apiKey(),golemLlm.model(),new PersistingGatheringDecisionSink());
        return decision.allowed();
    }
    @Override public void deposit(CopperGolem golem, ServerLevel level, List<ItemStack> storage) {
        GatheringHomeResolver.resolve(CopperGolemData.readEntityTag(golem),level)
                .ifPresent(home->GatheringHomeDeposit.tick(golem,level,home.binding().containerPos(),home.container(),storage));
    }
    @Override public void stop(CopperGolem golem) { golem.getNavigation().stop(); }
    @Override public void tickTarget(CopperGolem golem, ServerLevel level, CompoundTag tag, BlockPos target) {
        if (golem.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(target)) > 4D) {
            if (GatheringRuntimeState.setActivity(tag, CopperGolemActivity.MOVING_TO_TARGET)) CopperGolemData.writeEntityTag(golem,tag);
            GatheringNavigation.moveOrSkip(golem, level, target);
            return;
        }
        GatheringNavigation.forget(golem);
        GatheringRuntimeState.setActivity(tag, CopperGolemActivity.WORKING);
        var state = level.getBlockState(target);
        ItemStack tool = CopperGolemData.readItemStack(tag, TOOL, level.registryAccess());
        var progress = GatheringBreakProgress.advance(tag, level, target, state, tool);
        CopperGolemData.writeEntityTag(golem, tag);
        level.destroyBlockProgress(golem.getId(), target, progress.crackStage());
        if (progress.progressTicks() % 5 == 0) {
            level.levelEvent(2001, target, net.minecraft.world.level.block.Block.getId(state));
            golem.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
        }
        if (!progress.complete()) return;

        level.destroyBlockProgress(golem.getId(), target, -1);
        GatheringBlockBreaker.Result result = GatheringOperator.resolve(golem, level)
                .map(player -> GatheringBlockBreaker.breakTarget(golem, level, player, target))
                .orElse(GatheringBlockBreaker.Result.REJECTED);
        if (result == GatheringBlockBreaker.Result.BROKEN) {
            CopperGolemLifecycle.clearGatheringDisplayedItem(golem);
            CompoundTag updated = CopperGolemData.readEntityTag(golem);
            if (GatheringRuntimeState.setActivity(updated, CopperGolemActivity.SEARCHING)) {
                CopperGolemData.writeEntityTag(golem, updated);
            }
        } else if (result == GatheringBlockBreaker.Result.TOOL_BROKEN) {
            CopperGolemLifecycle.clearGatheringDisplayedItem(golem);
        } else {
            CompoundTag rejected = CopperGolemData.readEntityTag(golem);
            GatheringBreakProgress.clear(rejected);
            GatheringRuntimeState.deferTarget(
                    rejected,
                    level.getGameTime() + GatheringScanCursor.RETRY_TICKS
            );
            CopperGolemData.writeEntityTag(golem, rejected);
            CopperGolemLifecycle.clearGatheringDisplayedItem(golem);
            golem.getNavigation().stop();
        }
    }
}
