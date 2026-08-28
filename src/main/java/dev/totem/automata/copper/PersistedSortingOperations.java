package dev.totem.automata.copper;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/** Sorting operations backed by the migrated per-golem and per-binding LLM state. */
public class PersistedSortingOperations extends AbstractSortingOperations {
    protected final ItemMetadata items;
    public PersistedSortingOperations(ItemMetadata items) { this.items = items; }

    @Override public boolean acceptsByCachedDecision(CopperGolem golem, CopperGolemBinding binding, ItemStack carried) {
        SortingLlmState.Config config = SortingLlmState.get(CopperGolemData.readEntityTag(golem), binding);
        return acceptsByCachedDecision(config, carried);
    }

    @Override public boolean acceptsByCachedDecision(CopperGolem golem, CopperGolemBinding binding,
                                                      ItemStack carried, RouteSnapshot snapshot) {
        return acceptsByCachedDecision(SortingLlmState.get(snapshot.authorityTag(), binding), carried);
    }

    private boolean acceptsByCachedDecision(SortingLlmState.Config config, ItemStack carried) {
        return CachedSortingDecisionPolicy.decide(config.allowedItemIds(), config.deniedItemIds(), config.allowedTags(), config.deniedTags(),
                items.itemId(carried), items.itemTags(carried)).orElse(true);
    }

    @Override public boolean canAccept(CopperGolem golem, ServerLevel level, CopperGolemBinding binding, Container container, ItemStack carried) {
        SortingLlmState.Config config = SortingLlmState.get(CopperGolemData.readEntityTag(golem), binding);
        return canAccept(config, container, carried);
    }

    @Override public boolean canAccept(CopperGolem golem, ServerLevel level, CopperGolemBinding binding,
                                       Container container, ItemStack carried, RouteSnapshot snapshot) {
        SortingLlmState.Config config = SortingLlmState.get(snapshot.authorityTag(), binding);
        return canAccept(config, container, carried);
    }

    protected boolean canAccept(SortingLlmState.Config config, Container container, ItemStack carried) {
        Optional<Boolean> decision = CachedSortingDecisionPolicy.decide(config.allowedItemIds(), config.deniedItemIds(), config.allowedTags(), config.deniedTags(),
                items.itemId(carried), items.itemTags(carried));
        if (decision.orElse(false) && SortingDestinationService.hasAvailableSpace(container, carried)) return true;
        return decision.orElse(true) && SortingDestinationService.canAccept(container, carried);
    }
}
