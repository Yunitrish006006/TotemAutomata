package dev.totem.automata.copper;

import dev.totem.automata.containersafety.LocksmithAutomationBridge;
import net.minecraft.core.BlockPos;
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
    void clearRememberedSource(CopperGolem golem);
    boolean shouldClearBlocked(CopperGolem golem, ServerLevel level);
    void clearBlocked(CopperGolem golem);

    record Source(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension, BlockPos containerPos, int slot) { }
}
