package dev.totem.automata.copper;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class GatheringLlmStateTest {
    @Test void promptChangeBumpsRevisionAndClearsCachedDecisions() {
        CompoundTag tag = new CompoundTag(); GatheringLlmState.configure(tag, true, "mine ores");
        int revision = GatheringLlmState.read(tag).promptRevision();
        assertTrue(GatheringLlmState.recordDecision(tag, "minecraft:iron_ore", List.of("minecraft:ores"), true, List.of("minecraft:ores"), revision));
        GatheringLlmState.configure(tag, true, "mine gems");
        var config = GatheringLlmState.read(tag); assertEquals(revision + 1, config.promptRevision());
        assertTrue(config.allowedBlockIds().isEmpty()); assertTrue(config.allowedTags().isEmpty());
    }
    @Test void cachedBlockDecisionsPrecedeTagDecisionsAndRejectStaleResponses() {
        CompoundTag tag = new CompoundTag(); GatheringLlmState.configure(tag, true, "mine"); int revision = GatheringLlmState.read(tag).promptRevision();
        assertTrue(GatheringLlmState.recordDecision(tag, "minecraft:stone", List.of("minecraft:base_stone"), false, List.of("minecraft:base_stone"), revision));
        var config = GatheringLlmState.read(tag); assertEquals(false, GatheringLlmState.cachedDecision(config, "minecraft:stone", List.of("minecraft:base_stone")).orElseThrow());
        assertFalse(GatheringLlmState.recordDecision(tag, "minecraft:diamond_ore", List.of(), true, List.of(), revision + 1));
    }
}
