package dev.totem.automata.copper;

import net.minecraft.nbt.CompoundTag;
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
        CompoundTag tag = CopperGolemData.readEntityTag(golem);
        SortingLlmState.Config bindingConfig = SortingLlmState.get(tag, binding);
        Optional<Boolean> cached = CachedSortingDecisionPolicy.decide(bindingConfig.allowedItemIds(), bindingConfig.deniedItemIds(),
                bindingConfig.allowedTags(), bindingConfig.deniedTags(), items.itemId(carried), items.itemTags(carried));
        if (cached.isPresent() || super.canAccept(golem, level, binding, container, carried)) return super.canAccept(golem, level, binding, container, carried);
        GolemLlmState.Config golemConfig = GolemLlmState.read(tag);
        if (!bindingConfig.enabled() || bindingConfig.prompt().isBlank() || !golemConfig.configured()
                || !SortingDestinationService.hasAvailableSpace(container, carried)) return false;
        SortingLlmClassifier.requestClassification(level.getServer(), golem.getUUID(), binding, items.itemId(carried),
                items.itemName(carried), items.itemTags(carried), bindingConfig.prompt(), golemConfig.apiUrl(), golemConfig.apiKey(),
                golemConfig.model(), items.referenceTable(), decisionSink);
        return false;
    }
}
