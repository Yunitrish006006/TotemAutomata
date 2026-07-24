package dev.totem.automata.client;

import dev.totem.automata.network.CopperWrenchBindingsPayload;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CopperGolemMenuEditorTest {
    private static CopperWrenchBindingsPayload.BindingEntry binding() {
        return new CopperWrenchBindingsPayload.BindingEntry("minecraft:overworld", 1, 2, 3,
                "minecraft:chest", "minecraft:chest", true, true, false, " old ", 0, 0,
                List.of("minecraft:dirt"), List.of(), List.of("minecraft:logs"), List.of());
    }

    @Test void updatesBindingPromptWithoutChangingItsIdentityOrCache() {
        var updated = CopperGolemMenuEditor.updateBindingLlm(binding(), true, "  sort food  ");
        assertTrue(updated.llmEnabled());
        assertEquals("sort food", updated.llmPrompt());
        assertEquals("minecraft:overworld", updated.dimension());
        assertEquals(List.of("minecraft:dirt"), updated.llmAllowedItemIds());
    }

    @Test void movesCachedTagBetweenDecisionSidesWithoutDuplicates() {
        var allowed = CopperGolemMenuEditor.moveCachedDecision(binding(), "minecraft:logs", true, true);
        assertEquals(List.of("minecraft:logs"), allowed.llmAllowedTags());
        assertTrue(allowed.llmDeniedTags().isEmpty());
        var denied = CopperGolemMenuEditor.moveCachedDecision(allowed, "minecraft:logs", true, false);
        assertTrue(denied.llmAllowedTags().isEmpty());
        assertEquals(List.of("minecraft:logs"), denied.llmDeniedTags());
    }

    @Test void normalizesGatheringPrompt() {
        assertEquals(new CopperGolemMenuEditor.GatheringLlmSettings(true, "mine ores"),
                CopperGolemMenuEditor.updateGatheringLlm(true, " mine ores "));
    }
}
