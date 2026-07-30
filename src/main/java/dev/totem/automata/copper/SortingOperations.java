package dev.totem.automata.copper;

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
    OptionalInt sortableSourceSlot(CopperGolem golem, ServerLevel level, Container source, BlockPos sourcePos);
    boolean hasAnySortableItem(CopperGolem golem, ServerLevel level, Container source, BlockPos sourcePos);
    default boolean awaitingSortingDecision(CopperGolem golem, ServerLevel level, Container source, BlockPos sourcePos) {
        return false;
    }
    void markBlocked(CopperGolem golem, ServerLevel level, BlockPos sourcePos, Container source);
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
