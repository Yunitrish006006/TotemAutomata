package dev.totem.automata.client;

import dev.totem.automata.network.CopperWrenchBindingsPayload;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CopperGolemMenuClientControllerTest {
    private static CopperWrenchBindingsPayload snapshot() {
        var binding = new CopperWrenchBindingsPayload.BindingEntry("minecraft:overworld", 1, 2, 3, "minecraft:chest", "minecraft:chest", true, true, false, "", 0, 0, List.of(), List.of(), List.of(), List.of());
        return new CopperWrenchBindingsPayload(UUID.randomUUID(), 7, false, "sorting", "stopped", "minecraft:coal", 1, 1600, false, "minecraft:air", 0, 0, 0, "minecraft:air", 0, "", "", "", 0, null, null, List.of(), false, "", 0, 0, List.of(), List.of(), List.of(), List.of(), List.of(binding));
    }

    @Test void appliesOptimisticBindingEditAndReturnsLegacyCommandValues() {
        var controller = new CopperGolemMenuClientController(); controller.apply(snapshot());
        var command = controller.updateBindingLlm(0, true, "  sort food ").orElseThrow();
        assertTrue(command.enabled()); assertEquals("sort food", command.prompt()); assertEquals(7, command.revision());
        assertTrue(controller.snapshot().orElseThrow().bindings().getFirst().llmEnabled());
    }

    @Test void togglesOperationUsingFuelStateAndDoesNotInventStateBeforeSnapshot() {
        var controller = new CopperGolemMenuClientController(); assertTrue(controller.toggleOperation().isEmpty());
        controller.apply(snapshot());
        var command = controller.toggleOperation().orElseThrow();
        assertTrue(command.running()); assertEquals("searching", controller.snapshot().orElseThrow().activity());
    }
    @Test void normalizesApiConfigurationAndUsesTheCurrentRevision() {
        var controller = new CopperGolemMenuClientController(); controller.apply(snapshot());
        var command = controller.saveApiConfig(" https://api.example ", " key ", " model ").orElseThrow();
        assertEquals("https://api.example", command.apiUrl()); assertEquals("key", command.apiKey()); assertEquals("model", command.model()); assertEquals(7, command.revision());
        assertEquals("model", controller.snapshot().orElseThrow().llmModel());
    }

    @Test void movesTrimmedCachedDecisionsUsingTheSelectedBindingRevision() {
        var controller = new CopperGolemMenuClientController(); controller.apply(snapshot());
        var command = controller.moveCachedDecision(0, " minecraft:logs ", true, false).orElseThrow();
        assertEquals("minecraft:logs", command.value()); assertTrue(command.tag()); assertFalse(command.allowed()); assertEquals(7, command.revision());
        var binding = controller.snapshot().orElseThrow().bindings().getFirst();
        assertEquals(List.of("minecraft:logs"), binding.llmDeniedTags());
        assertTrue(binding.llmAllowedTags().isEmpty());
    }
}
