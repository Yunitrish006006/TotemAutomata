package dev.totem.automata.copper;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/** Stable cross-restart reference to a Copper Golem source or destination container. */
public record CopperGolemBinding(ResourceKey<Level> dimension, BlockPos containerPos) {
}
