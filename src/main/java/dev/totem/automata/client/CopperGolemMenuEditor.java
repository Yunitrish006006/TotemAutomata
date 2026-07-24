package dev.totem.automata.client;

import dev.totem.automata.network.CopperWrenchBindingsPayload;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure client-side edits for the Copper Golem menu snapshot.
 *
 * <p>The screen applies the returned values optimistically, then sends the
 * corresponding serverbound payload.  Keeping this logic free of rendering
 * and Fabric networking lets the eventual Automata screen retain the legacy
 * behaviour without depending on DeadRecall's screen class.</p>
 */
public final class CopperGolemMenuEditor {
    private CopperGolemMenuEditor() { }

    public static CopperWrenchBindingsPayload.BindingEntry updateBindingLlm(
            CopperWrenchBindingsPayload.BindingEntry entry, boolean enabled, String prompt) {
        return new CopperWrenchBindingsPayload.BindingEntry(
                entry.dimension(), entry.x(), entry.y(), entry.z(), entry.blockId(), entry.itemId(),
                entry.loaded(), entry.available(), enabled, normalize(prompt), entry.llmCachedItemIds(),
                entry.llmCachedTags(), entry.llmAllowedItemIds(), entry.llmDeniedItemIds(),
                entry.llmAllowedTags(), entry.llmDeniedTags());
    }

    public static CopperWrenchBindingsPayload.BindingEntry moveCachedDecision(
            CopperWrenchBindingsPayload.BindingEntry entry, String value, boolean tag, boolean allowed) {
        List<String> allowedItems = new ArrayList<>(entry.llmAllowedItemIds());
        List<String> deniedItems = new ArrayList<>(entry.llmDeniedItemIds());
        List<String> allowedTags = new ArrayList<>(entry.llmAllowedTags());
        List<String> deniedTags = new ArrayList<>(entry.llmDeniedTags());
        if (tag) move(value, allowed, allowedTags, deniedTags);
        else move(value, allowed, allowedItems, deniedItems);
        return new CopperWrenchBindingsPayload.BindingEntry(
                entry.dimension(), entry.x(), entry.y(), entry.z(), entry.blockId(), entry.itemId(),
                entry.loaded(), entry.available(), entry.llmEnabled(), entry.llmPrompt(), entry.llmCachedItemIds(),
                entry.llmCachedTags(), allowedItems, deniedItems, allowedTags, deniedTags);
    }

    public static GatheringLlmSettings updateGatheringLlm(boolean enabled, String prompt) {
        return new GatheringLlmSettings(enabled, normalize(prompt));
    }

    private static void move(String value, boolean allowed, List<String> allowedValues, List<String> deniedValues) {
        if (value == null || value.isBlank()) return;
        if (allowed) {
            if (!allowedValues.contains(value)) allowedValues.add(value);
            deniedValues.remove(value);
        } else {
            if (!deniedValues.contains(value)) deniedValues.add(value);
            allowedValues.remove(value);
        }
    }

    private static String normalize(String value) { return value == null ? "" : value.trim(); }

    public record GatheringLlmSettings(boolean enabled, String prompt) { }
}
