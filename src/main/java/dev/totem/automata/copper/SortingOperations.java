package dev.totem.automata.copper;

import dev.totem.automata.containersafety.LocksmithAutomationBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ai.behavior.TransportItemsBetweenContainers.TransportItemTarget;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

/** Live-state operations required by the extracted sorting controller. */
public interface SortingOperations {
    List<CopperGolemBinding> bindings(CopperGolem golem);
    List<CopperGolemBinding> triedDestinations(CopperGolem golem);
    Optional<Source> rememberedSource(CopperGolem golem);
    TransportItemTarget target(ServerLevel level, BlockPos pos);
    boolean canAccept(CopperGolem golem, ServerLevel level, CopperGolemBinding binding, Container container, ItemStack carried);
    void rememberTriedDestination(CopperGolem golem, CopperGolemBinding binding);
    boolean hasFuel(CopperGolem golem, ServerLevel level);

    /**
     * Access policy must run before source inventory inspection or LLM
     * classification. Tests may override this seam to prove that ordering.
     */
    default boolean mayExtract(CopperGolem golem, Container source) {
        return LocksmithAutomationBridge.mayExtract(
                source,
                GatheringOperator.operatorId(golem).orElse(null)
        );
    }

    default boolean mayTransfer(
            CopperGolem golem,
            ServerLevel level,
            BlockPos source,
            BlockPos destination
    ) {
        return LocksmithAutomationBridge.mayTransfer(
                level, source, destination,
                GatheringOperator.operatorId(golem).orElse(null));
    }

    default boolean mayInsert(CopperGolem golem, Container destination) {
        return LocksmithAutomationBridge.mayInsert(
                destination,
                GatheringOperator.operatorId(golem).orElse(null));
    }

    /**
     * Returning a remainder normally crosses an INSERT boundary. If the
     * original transfer was internal to one Lock UUID, its reverse route is
     * also authorised even when that lock's boundary mode is DENY.
     */
    default boolean mayReturnToSource(
            CopperGolem golem,
            ServerLevel level,
            BlockPos sourcePosition,
            Container source
    ) {
        return mayReturnToSource(golem, level, sourcePosition, source, routeSnapshot(golem));
    }

    default boolean mayReturnToSource(
            CopperGolem golem,
            ServerLevel level,
            BlockPos sourcePosition,
            Container source,
            RouteSnapshot snapshot
    ) {
        if (mayInsert(golem, source)) return true;
        return snapshot.triedDestinations().stream()
                .filter(binding -> binding.dimension().equals(level.dimension()))
                .anyMatch(binding -> mayTransfer(
                        golem, level, binding.containerPos(), sourcePosition));
    }

    OptionalInt sortableSourceSlot(CopperGolem golem, ServerLevel level, Container source, BlockPos sourcePos);
    boolean hasAnySortableItem(CopperGolem golem, ServerLevel level, Container source, BlockPos sourcePos);
    default boolean awaitingSortingDecision(CopperGolem golem, ServerLevel level, Container source, BlockPos sourcePos) {
        return false;
    }
    void markBlocked(CopperGolem golem, ServerLevel level, BlockPos sourcePos, Container source);

    /** Marks a permission-denied source without reading or hashing its contents. */
    void markAccessBlocked(CopperGolem golem, ServerLevel level, BlockPos sourcePos);

    int maxTransportStackSize();
    void rememberSource(CopperGolem golem, ServerLevel level, BlockPos sourcePos, int slot);
    void consumeFuel(CopperGolem golem, ServerLevel level);
    ItemStack returnToSource(Container source, ItemStack carried, int sourceSlot);
    boolean acceptsByCachedDecision(CopperGolem golem, CopperGolemBinding binding, ItemStack carried);
    default boolean acceptsByCachedDecision(CopperGolem golem, CopperGolemBinding binding,
                                             ItemStack carried, RouteSnapshot snapshot) {
        return acceptsByCachedDecision(golem, binding, carried);
    }
    void clearRememberedSource(CopperGolem golem);
    boolean shouldClearBlocked(CopperGolem golem, ServerLevel level);
    void clearBlocked(CopperGolem golem);

    /**
     * Captures all route authority fields from one entity-tag read. Runtime
     * implementations override this; the default keeps lightweight test
     * doubles source-compatible.
     */
    default RouteSnapshot routeSnapshot(CopperGolem golem) {
        return new RouteSnapshot(
                bindings(golem),
                triedDestinations(golem),
                rememberedSource(golem),
                new CompoundTag()
        );
    }

    default boolean canAccept(
            CopperGolem golem,
            ServerLevel level,
            CopperGolemBinding binding,
            Container container,
            ItemStack carried,
            RouteSnapshot snapshot
    ) {
        return canAccept(golem, level, binding, container, carried);
    }

    default void rememberTriedDestinations(CopperGolem golem, List<CopperGolemBinding> bindings) {
        bindings.forEach(binding -> rememberTriedDestination(golem, binding));
    }

    default boolean blockedRetryDue(CopperGolem golem) {
        return golem.tickCount % SortingBlockedBackoff.INITIAL_DELAY_TICKS == 0;
    }

    default void advanceBlockedRetry(CopperGolem golem) {
    }

    record Source(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension, BlockPos containerPos, int slot) { }
    record RouteSnapshot(
            List<CopperGolemBinding> bindings,
            List<CopperGolemBinding> triedDestinations,
            Optional<Source> rememberedSource,
            CompoundTag authorityTag
    ) {
        public RouteSnapshot {
            bindings = List.copyOf(bindings);
            triedDestinations = List.copyOf(triedDestinations);
        }
    }
}
