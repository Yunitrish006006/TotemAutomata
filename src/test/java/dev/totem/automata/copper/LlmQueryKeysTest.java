package dev.totem.automata.copper;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class LlmQueryKeysTest {
    @Test
    void keysCanonicalizeTagsAndSeparatePromptGenerations() {
        UUID golemId = UUID.fromString("1e8fa638-3cf3-4fbb-a989-65fd29cd708f");
        CopperGolemBinding binding = new CopperGolemBinding(Level.OVERWORLD, new BlockPos(4, 70, -3));
        String firstSorting = LlmQueryKeys.sorting(golemId, binding, "minecraft:diamond",
                List.of("minecraft:z", "minecraft:a", "minecraft:a"), "ores");
        String reorderedSorting = LlmQueryKeys.sorting(golemId, binding, "minecraft:diamond",
                List.of("minecraft:a", "minecraft:z"), "ores");
        String changedSortingPrompt = LlmQueryKeys.sorting(golemId, binding, "minecraft:diamond",
                List.of("minecraft:a", "minecraft:z"), "tools");
        assertEquals(firstSorting, reorderedSorting);
        assertNotEquals(firstSorting, changedSortingPrompt);

        assertNotEquals(
                LlmQueryKeys.gathering(golemId, "minecraft:stone", List.of("minecraft:a"), 3),
                LlmQueryKeys.gathering(golemId, "minecraft:stone", List.of("minecraft:a"), 4));
    }
}
