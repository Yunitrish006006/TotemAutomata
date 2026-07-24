package dev.totem.automata.copper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class CopperWrenchInteractionDebounceTest {
    @Test void suppressesOnlyTheImmediateEntityToBlockFollowup() {
        var debounce = new CopperWrenchInteractionDebounce(); UUID player = UUID.randomUUID();
        debounce.recordEntityUse(player, InteractionHand.MAIN_HAND, false, 10);
        assertTrue(debounce.consumeEntityToBlockSuppression(player, InteractionHand.MAIN_HAND, false, 12));
        assertFalse(debounce.consumeEntityToBlockSuppression(player, InteractionHand.MAIN_HAND, false, 12));
    }
    @Test void suppressesRepeatedGatheringTargetClicksForEightTicks() {
        var debounce = new CopperWrenchInteractionDebounce(); UUID player = UUID.randomUUID(), golem = UUID.randomUUID();
        ResourceKey<Level> overworld = ResourceKey.create(Registries.DIMENSION, Identifier.fromNamespaceAndPath("minecraft", "overworld"));
        assertFalse(debounce.isGatheringTargetDuplicate(player, golem, overworld, BlockPos.ZERO, "minecraft:stone", 10));
        assertTrue(debounce.isGatheringTargetDuplicate(player, golem, overworld, BlockPos.ZERO, "minecraft:stone", 18));
        assertFalse(debounce.isGatheringTargetDuplicate(player, golem, overworld, BlockPos.ZERO, "minecraft:stone", 27));
    }
}
