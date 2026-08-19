package dev.totem.automata.copper;

import dev.totem.automata.containersafety.LocksmithAutomationBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Concrete carried-storage return and deposit at the configured gathering home. */
public final class GatheringHomeDeposit {
    private GatheringHomeDeposit() { }
    public static Result tick(CopperGolem golem, ServerLevel level, BlockPos homePos, Container home, List<ItemStack> storage) {
        if (golem.distanceToSqr(Vec3.atCenterOf(homePos)) > 6.25D) {
            setActivity(golem, CopperGolemActivity.RETURNING_HOME);
            golem.getNavigation().moveTo(homePos.getX()+.5D, homePos.getY(), homePos.getZ()+.5D, .75D);
            return Result.MOVING;
        }
        setActivity(golem, CopperGolemActivity.DEPOSITING);
        if (!LocksmithAutomationBridge.mayInsert(home, GatheringOperator.operatorId(golem).orElse(null))) {
            setActivity(golem, CopperGolemActivity.BLOCKED_HOME_FULL);
            golem.getNavigation().stop();
            return Result.BLOCKED_FULL;
        }
        if (!GatheringDeposit.canInsertAll(home, storage)) {
            setActivity(golem, CopperGolemActivity.BLOCKED_HOME_FULL);
            golem.getNavigation().stop();
            return Result.BLOCKED_FULL;
        }
        if (!GatheringDeposit.insertAll(home, storage)) {
            setActivity(golem, CopperGolemActivity.BLOCKED_HOME_FULL);
            return Result.BLOCKED_FULL;
        }
        home.setChanged();
        CompoundTag tag = CopperGolemData.readEntityTag(golem);
        GatheringStorage.write(tag, List.of(), level.registryAccess());
        GatheringRuntimeState.setActivity(tag, CopperGolemActivity.SEARCHING);
        CopperGolemData.writeEntityTag(golem, tag);
        return Result.DEPOSITED;
    }
    private static void setActivity(CopperGolem golem, CopperGolemActivity activity) {
        CompoundTag tag = CopperGolemData.readEntityTag(golem);
        GatheringRuntimeState.setActivity(tag, activity);
        CopperGolemData.writeEntityTag(golem, tag);
    }
    public enum Result { MOVING, DEPOSITED, BLOCKED_FULL }
}
