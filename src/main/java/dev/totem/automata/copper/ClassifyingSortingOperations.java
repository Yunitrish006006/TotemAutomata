package dev.totem.automata.copper;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/** Persisted sorting operations that request an LLM decision only for a true cache miss. */
public final class ClassifyingSortingOperations extends PersistedSortingOperations {
    private final SortingDecisionSink decisionSink;
    public ClassifyingSortingOperations(ItemMetadata items, SortingDecisionSink decisionSink) {
        super(items); this.decisionSink = decisionSink;
    }

    @Override public boolean canAccept(CopperGolem golem, ServerLevel level, CopperGolemBinding binding, Container container, ItemStack carried) {
        return canAccept(CopperGolemData.readEntityTag(golem), golem, level, binding, container, carried);
    }

    @Override public boolean canAccept(CopperGolem golem, ServerLevel level, CopperGolemBinding binding,
                                       Container container, ItemStack carried, RouteSnapshot snapshot) {
        return canAccept(snapshot.authorityTag(), golem, level, binding, container, carried);
    }

    private boolean canAccept(CompoundTag tag, CopperGolem golem, ServerLevel level,
                              CopperGolemBinding binding, Container container, ItemStack carried) {
        SortingLlmState.Config bindingConfig = SortingLlmState.get(tag, binding);
        Optional<Boolean> cached = CachedSortingDecisionPolicy.decide(bindingConfig.allowedItemIds(), bindingConfig.deniedItemIds(),
                bindingConfig.allowedTags(), bindingConfig.deniedTags(), items.itemId(carried), items.itemTags(carried));
        if (cached.isPresent()) return super.canAccept(bindingConfig, container, carried);
        GolemLlmState.Config golemConfig = GolemLlmState.read(tag);
        if (!classificationEnabled(bindingConfig, golemConfig)) return super.canAccept(bindingConfig, container, carried);
        if (!SortingDestinationService.hasAvailableSpace(container, carried)) return false;
        SortingLlmClassifier.requestClassification(level.getServer(), golem.getUUID(), binding, items.itemId(carried),
                items.itemName(carried), items.itemTags(carried), bindingConfig.prompt(), golemConfig.apiUrl(), golemConfig.apiKey(),
                golemConfig.model(), items.referenceTable(), decisionSink);
        return false;
    }

    @Override public boolean awaitingSortingDecision(CopperGolem golem, ServerLevel level, Container source, BlockPos sourcePos) {
        CompoundTag tag = CopperGolemData.readEntityTag(golem);
        GolemLlmState.Config golemConfig = GolemLlmState.read(tag);
        if (!golemConfig.configured()) return false;
        for (int slot = 0; slot < source.getContainerSize(); slot++) {
            ItemStack stack = source.getItem(slot);
            if (stack.isEmpty()) continue;
            ItemStack candidate = stack.copyWithCount(Math.min(stack.getCount(), maxTransportStackSize()));
            for (CopperGolemBinding binding : bindings(golem)) {
                if (!binding.dimension().equals(level.dimension()) || binding.containerPos().equals(sourcePos)) continue;
                SortingLlmState.Config bindingConfig = SortingLlmState.get(tag, binding);
                if (!classificationEnabled(bindingConfig, golemConfig) || hasCachedDecision(bindingConfig, candidate)) continue;
                var target = target(level, binding.containerPos());
                if (target != null && SortingDestinationService.hasAvailableSpace(target.container(), candidate)) return true;
            }
        }
        return false;
    }

    private boolean hasCachedDecision(SortingLlmState.Config config, ItemStack carried) {
        return CachedSortingDecisionPolicy.decide(config.allowedItemIds(), config.deniedItemIds(), config.allowedTags(), config.deniedTags(),
                items.itemId(carried), items.itemTags(carried)).isPresent();
    }

    private static boolean classificationEnabled(SortingLlmState.Config bindingConfig, GolemLlmState.Config golemConfig) {
        return bindingConfig.enabled() && !bindingConfig.prompt().isBlank() && golemConfig.configured();
    }
}
