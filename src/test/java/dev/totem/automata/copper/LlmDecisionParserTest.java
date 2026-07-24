package dev.totem.automata.copper;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LlmDecisionParserTest {
    @Test
    void parserStripsReasoningAndFiltersUnprovidedTags() {
        LlmDecisionParser.Decision decision = LlmDecisionParser.parse("""
                <think>internal reasoning</think>
                ```json
                {"match":true,"tags":["minecraft:ores","minecraft:forged","minecraft:ores"]}
                ```
                """, List.of("minecraft:ores", "minecraft:gems"));
        assertTrue(decision.matches());
        assertEquals(List.of("minecraft:ores"), decision.tags());
    }
}
