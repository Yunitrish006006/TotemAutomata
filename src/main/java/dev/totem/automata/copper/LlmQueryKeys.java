package dev.totem.automata.copper;

import java.util.List;
import java.util.UUID;

/** Canonical request keys used to deduplicate asynchronous item and block classification. */
public final class LlmQueryKeys {
    private LlmQueryKeys() {
    }

    public static String sorting(
            UUID golemId, CopperGolemBinding binding, String itemId, List<String> itemTags, String prompt) {
        return golemId + "|" + binding.dimension().identifier() + "|" + binding.containerPos().asLong()
                + "|prompt|" + (prompt == null ? "" : prompt.trim()) + "|" + itemId + "|" + canonicalTags(itemTags);
    }

    public static String gathering(UUID golemId, String blockId, List<String> blockTags, int promptRevision) {
        return golemId + "|block|" + promptRevision + "|" + blockId + "|" + canonicalTags(blockTags);
    }

    public static String canonicalTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) return "";
        return String.join(",", tags.stream()
                .filter(tag -> tag != null && !tag.isBlank())
                .distinct()
                .sorted()
                .toList());
    }
}
