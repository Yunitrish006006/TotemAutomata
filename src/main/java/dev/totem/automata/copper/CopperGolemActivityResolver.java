package dev.totem.automata.copper;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.phys.Vec3;

/** Derives the legacy menu activity from persisted state without a DeadRecall runtime dependency. */
public final class CopperGolemActivityResolver {
    private static final String SORTING_BLOCKED = "deadrecall_sorting_blocked";
    private static final String GATHERING_TOOL = "deadrecall_gathering_tool_stack";

    private CopperGolemActivityResolver() {
    }

    public static CopperGolemActivity resolveAndPersist(CopperGolem golem, ServerLevel level, CompoundTag tag) {
        CopperGolemActivity activity = resolve(golem, level, tag);
        tag.putString(CopperGolemData.TAG_ACTIVITY, activity.id());
        return activity;
    }

    public static CopperGolemActivity resolve(CopperGolem golem, ServerLevel level, CompoundTag tag) {
        if (!tag.getBooleanOr(CopperGolemData.TAG_TRANSPORT_ENABLED, false)) return CopperGolemActivity.STOPPED;
        if (SortingBindingService.getSourceContainer(tag).isEmpty()) return CopperGolemActivity.BLOCKED_NO_HOME;
        if (CopperGolemData.mode(tag) == CopperGolemMode.SORTING) {
            if (tag.getBooleanOr(SORTING_BLOCKED, false)) return CopperGolemActivity.BLOCKED_SORTING;
            if (golem.getMainHandItem().isEmpty() && !CopperGolemFuelService.hasFuelAvailable(tag, level)) return CopperGolemActivity.BLOCKED_NO_FUEL;
            return golem.getMainHandItem().isEmpty() ? CopperGolemActivity.SEARCHING : CopperGolemActivity.MOVING_TO_TARGET;
        }

        var storage = GatheringStorage.read(tag, level.registryAccess());
        CopperGolemActivity stored = CopperGolemData.activity(tag);
        if (!storage.isEmpty()) {
            if (stored == CopperGolemActivity.BLOCKED_HOME_UNAVAILABLE
                    || stored == CopperGolemActivity.BLOCKED_HOME_FULL
                    || stored == CopperGolemActivity.RETURNING_HOME
                    || stored == CopperGolemActivity.DEPOSITING) return stored;
            if (GatheringStorage.full(storage)) return CopperGolemActivity.RETURNING_HOME;
        }
        if (CopperGolemData.readItemStack(tag, GATHERING_TOOL, level.registryAccess()).isEmpty()) {
            return stored == CopperGolemActivity.BLOCKED_TOOL_BROKEN ? stored : CopperGolemActivity.BLOCKED_NO_TOOL;
        }
        if (!CopperGolemFuelService.hasFuelAvailable(tag, level)) return CopperGolemActivity.BLOCKED_NO_FUEL;
        if (GatheringConfiguration.scanBounds(tag, level.dimension()).isEmpty()) return CopperGolemActivity.BLOCKED_NO_AREA;
        if (!hasTargetRules(tag)) return CopperGolemActivity.BLOCKED_NO_VALID_TARGET;
        if (stored == CopperGolemActivity.BLOCKED_HOME_UNAVAILABLE
                || stored == CopperGolemActivity.BLOCKED_HOME_FULL
                || stored == CopperGolemActivity.BLOCKED_NO_VALID_TARGET) return stored;
        return GatheringRuntimeState.target(tag)
                .map(target -> isAtGatheringTarget(golem, target) ? CopperGolemActivity.WORKING : CopperGolemActivity.MOVING_TO_TARGET)
                .orElse(CopperGolemActivity.SEARCHING);
    }

    private static boolean hasTargetRules(CompoundTag tag) {
        return !GatheringConfiguration.manualTargets(tag).isEmpty() || GatheringLlmState.read(tag).usable(GolemLlmState.read(tag));
    }

    public static boolean isAtGatheringTarget(CopperGolem golem, BlockPos pos) {
        BlockPos head = golem.blockPosition().above();
        int upwardX = Math.abs(head.getX() - pos.getX());
        int upwardY = pos.getY() - head.getY();
        int upwardZ = Math.abs(head.getZ() - pos.getZ());
        if (upwardY >= 1 && upwardY <= 2 && upwardX <= 1 && upwardZ <= 1) return true;
        BlockPos feet = golem.blockPosition();
        int downwardX = Math.abs(feet.getX() - pos.getX());
        int downwardY = feet.getY() - pos.getY();
        int downwardZ = Math.abs(feet.getZ() - pos.getZ());
        return (downwardY >= 1 && downwardY <= 2 && downwardX <= 1 && downwardZ <= 1)
                || golem.distanceToSqr(Vec3.atCenterOf(pos)) <= 4.0D;
    }
}
