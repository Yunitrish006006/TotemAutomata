package dev.totem.automata.copper;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CachedSortingDecisionPolicyTest {
    @Test void itemRulesWinBeforeTagsAndAllowedTagsWinBeforeDeniedTags() {
        assertEquals(false, CachedSortingDecisionPolicy.decide(List.of(), List.of("minecraft:diamond"),
                List.of("minecraft:gems"), List.of(), "minecraft:diamond", List.of("minecraft:gems")).orElseThrow());
        assertEquals(true, CachedSortingDecisionPolicy.decide(List.of(), List.of(), List.of("minecraft:ores"),
                List.of("minecraft:ores"), "minecraft:iron_ore", List.of("minecraft:ores")).orElseThrow());
    }
}
