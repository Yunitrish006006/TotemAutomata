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

    private CopperGolemSortingAuthority() {
    }

    /** One CustomData snapshot for a mixin authority decision. */
    public static State snapshot(CopperGolem golem) {
        return stateFromOwnedTag(CopperGolemData.readEntityTag(golem));
    }

    static State snapshot(CompoundTag tag) {
        return stateFromOwnedTag(tag.copy());
    }

    private static State stateFromOwnedTag(CompoundTag tag) {
        return new State(
                CopperGolemData.mode(tag) == CopperGolemMode.SORTING,
                !SortingBindingService.getBindings(tag).isEmpty(),
                tag.getBooleanOr(CopperGolemData.TAG_TRANSPORT_ENABLED, false),
                tag.getBooleanOr(SORTING_BLOCKED, false),
                SortingBindingService.getSourceContainer(tag),
                tag
        );
    }

    public static boolean sortingMode(CopperGolem golem) {
        return snapshot(golem).sortingMode();
    }

    public static boolean hasBinding(CopperGolem golem) {
        return snapshot(golem).hasBinding();
    }

    public static boolean transportEnabled(CopperGolem golem) {
        return snapshot(golem).transportEnabled();
    }

    public static boolean sortingBlocked(CopperGolem golem) {
        return snapshot(golem).sortingBlocked();
    }

    public static boolean hasSource(CopperGolem golem) {
        return snapshot(golem).hasSource();
    }

    public static boolean sourceAt(CopperGolem golem, ServerLevel level, BlockPos pos) {
        return snapshot(golem).sourceAt(level, pos);
    }

    public static boolean hasFuel(CopperGolem golem, ServerLevel level) {
        return snapshot(golem).hasFuel(level);
    }

    public static Optional<TransportItemTarget> nextDestination(CopperGolem golem, ServerLevel level, ItemStack carried) {
        return SortingModeController.nextDestination(golem, level, carried, operations());
    }

    public static boolean returnCarried(CopperGolem golem, ServerLevel level) {
        return SortingModeController.returnCarried(golem, level, operations());
    }

    public static ItemStack pickUp(CopperGolem golem, ServerLevel level, Container source, BlockPos sourcePos) {
        return SortingModeController.pickUp(golem, level, source, sourcePos, operations());
    }

    public static Optional<ItemStack> deposit(CopperGolem golem, ServerLevel level, BlockPos targetPos, Container destination) {
        return SortingModeController.deposit(golem, level, targetPos, destination, operations());
    }

    public static void clearRememberedSource(CopperGolem golem) {
        operations().clearRememberedSource(golem);
    }

    private static SortingOperations operations() {
        return OperationsHolder.INSTANCE;
    }

    private static final class OperationsHolder {
        private static final SortingOperations INSTANCE =
                new ClassifyingSortingOperations(new DefaultItemMetadata(), new PersistingSortingDecisionSink());
    }

    public record State(
            boolean sortingMode,
            boolean hasBinding,
            boolean transportEnabled,
            boolean sortingBlocked,
            Optional<CopperGolemBinding> source,
            CompoundTag authorityTag
    ) {
        public State {
            source = source.map(binding -> new CopperGolemBinding(
                    binding.dimension(), binding.containerPos().immutable()));
        }

        public boolean hasSource() {
            return source.isPresent();
        }

        public boolean sourceAt(ServerLevel level, BlockPos pos) {
            return source.filter(binding -> binding.dimension().equals(level.dimension())
                    && binding.containerPos().equals(pos)).isPresent();
        }

        public boolean hasFuel(ServerLevel level) {
            return CopperGolemFuelService.hasFuelAvailable(authorityTag, level);
        }
    }
}
