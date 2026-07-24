package dev.totem.automata.copper;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class GatheringTargetPolicyTest {
    private static GatheringLlmState.Config configuredLlm() {
        CompoundTag tag = new CompoundTag(); GatheringLlmState.configure(tag, true, "mine ores"); return GatheringLlmState.read(tag);
    }
    private static final GolemLlmState.Config GOLEM = new GolemLlmState.Config("https://example.invalid", "key", "model");

    @Test void manualTargetsWinBeforeLlmConfigurationAndCache() {
        var decision = GatheringTargetPolicy.decide("minecraft:stone", List.of(), List.of("minecraft:stone"), configuredLlm(), GOLEM);
        assertEquals(GatheringTargetPolicy.Decision.ALLOW_MANUAL, decision);
    }
    @Test void usesCachedDecisionBeforeRequestingClassification() {
        CompoundTag tag = new CompoundTag(); GatheringLlmState.configure(tag, true, "mine ores");
        int revision = GatheringLlmState.read(tag).promptRevision();
        GatheringLlmState.recordDecision(tag, "minecraft:iron_ore", List.of(), true, List.of(), revision);
        assertEquals(GatheringTargetPolicy.Decision.ALLOW_CACHE, GatheringTargetPolicy.decide("minecraft:iron_ore", List.of(), List.of(), GatheringLlmState.read(tag), GOLEM));
        assertEquals(GatheringTargetPolicy.Decision.REQUEST_CLASSIFICATION, GatheringTargetPolicy.decide("minecraft:gold_ore", List.of(), List.of(), GatheringLlmState.read(tag), GOLEM));
    }
    @Test void deniesWhenNeitherManualNorUsableLlmRuleExists() {
        assertEquals(GatheringTargetPolicy.Decision.DENY_NO_RULE, GatheringTargetPolicy.decide("minecraft:stone", List.of(), List.of(), configuredLlm(), new GolemLlmState.Config("", "", "")));
        assertFalse(GatheringTargetPolicy.hasRules(List.of(), configuredLlm(), new GolemLlmState.Config("", "", "")));
    }
}
