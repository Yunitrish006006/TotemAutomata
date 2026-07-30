package dev.totem.automata.copper;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ai.behavior.TransportItemsBetweenContainers.TransportItemTarget;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/** External facade used by the cutover-only vanilla transport mixin. */
public final class CopperGolemSortingAuthority {
    private static final String SORTING_BLOCKED = "deadrecall_sorting_blocked";
    private static final SortingOperations OPERATIONS =
            new ClassifyingSortingOperations(new DefaultItemMetadata(), new PersistingSortingDecisionSink());

    private CopperGolemSortingAuthority() {
    }

    public static boolean sortingMode(CopperGolem golem) {
        return CopperGolemData.mode(CopperGolemData.readEntityTag(golem)) == CopperGolemMode.SORTING;
    }

    public static boolean hasBinding(CopperGolem golem) {
        return !SortingBindingService.getBindings(CopperGolemData.readEntityTag(golem)).isEmpty();
    }

    public static boolean transportEnabled(CopperGolem golem) {
        return CopperGolemData.readEntityTag(golem).getBooleanOr(CopperGolemData.TAG_TRANSPORT_ENABLED, false);
    }

    public static boolean sortingBlocked(CopperGolem golem) {
        return CopperGolemData.readEntityTag(golem).getBooleanOr(SORTING_BLOCKED, false);
    }

    public static boolean hasSource(CopperGolem golem) {
        return SortingBindingService.getSourceContainer(CopperGolemData.readEntityTag(golem)).isPresent();
    }

    public static boolean sourceAt(CopperGolem golem, ServerLevel level, BlockPos pos) {
        return SortingBindingService.isSourceContainer(CopperGolemData.readEntityTag(golem), level, pos);
    }

    public static boolean hasFuel(CopperGolem golem, ServerLevel level) {
        return CopperGolemFuelService.hasFuelAvailable(CopperGolemData.readEntityTag(golem), level);
    }

    public static Optional<TransportItemTarget> nextDestination(CopperGolem golem, ServerLevel level, ItemStack carried) {
        return SortingModeController.nextDestination(golem, level, carried, OPERATIONS);
    }

    public static boolean returnCarried(CopperGolem golem, ServerLevel level) {
        return SortingModeController.returnCarried(golem, level, OPERATIONS);
    }

    public static ItemStack pickUp(CopperGolem golem, ServerLevel level, Container source, BlockPos sourcePos) {
        return SortingModeController.pickUp(golem, level, source, sourcePos, OPERATIONS);
    }

    public static Optional<ItemStack> deposit(CopperGolem golem, ServerLevel level, BlockPos targetPos, Container destination) {
        return SortingModeController.deposit(golem, level, targetPos, destination, OPERATIONS);
    }

    public static void clearRememberedSource(CopperGolem golem) {
        OPERATIONS.clearRememberedSource(golem);
    }
}
