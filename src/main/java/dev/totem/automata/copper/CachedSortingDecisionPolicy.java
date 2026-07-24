package dev.totem.automata.copper;

import java.util.List;
import java.util.Optional;

/** Pure legacy-compatible allow/deny precedence for a binding's LLM cache. */
public final class CachedSortingDecisionPolicy {
    private CachedSortingDecisionPolicy() { }

    public static Optional<Boolean> decide(List<String> allowedItemIds, List<String> deniedItemIds,
                                           List<String> allowedTags, List<String> deniedTags,
                                           String itemId, List<String> itemTags) {
        if (allowedItemIds.contains(itemId)) return Optional.of(true);
        if (deniedItemIds.contains(itemId)) return Optional.of(false);
        for (String tag : itemTags) if (allowedTags.contains(tag)) return Optional.of(true);
        for (String tag : itemTags) if (deniedTags.contains(tag)) return Optional.of(false);
        return Optional.empty();
    }
}
