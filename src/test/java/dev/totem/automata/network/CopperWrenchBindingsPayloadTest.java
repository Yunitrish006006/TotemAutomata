package dev.totem.automata.network;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CopperWrenchBindingsPayloadTest {
    @Test
    void preservesLegacyIdentifierAndCompleteSnapshotShape() {
        CopperWrenchBindingsPayload.BindingEntry binding = new CopperWrenchBindingsPayload.BindingEntry(
                "minecraft:overworld", 4, 70, -2, "minecraft:chest", "minecraft:diamond", true, true,
                true, "sort gems", 2, 1, List.of("minecraft:diamond"), List.of(), List.of("minecraft:gems"), List.of());
        CopperWrenchBindingsPayload payload = new CopperWrenchBindingsPayload(
                UUID.randomUUID(), 4, true, "sorting", "transporting", "minecraft:nether_star", 3, 120, true,
                "minecraft:iron_pickaxe", 1, 2, 250, "minecraft:cobblestone", 16,
                "https://example.invalid/v1", "secret", "model", 1, binding,
                new CopperWrenchBindingsPayload.GatheringAreaEntry("minecraft:overworld", true, 0, 64, 0,
                        true, 8, 70, 8), List.of("minecraft:stone"), true, "mine stone", 2, 1,
                List.of("minecraft:stone"), List.of(), List.of("minecraft:base_stone_overworld"), List.of(),
                List.of(binding));

        assertEquals("deadrecall:copper_wrench_bindings", payload.type().id().toString());
        assertEquals(1, payload.bindings().size());
        assertEquals("minecraft:overworld", payload.gatheringArea().dimension());
        assertEquals("minecraft:diamond", payload.bindings().getFirst().itemId());
        assertTrue(payload.infiniteFuel());
    }
}
