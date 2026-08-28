package dev.totem.automata.copper;

import net.minecraft.core.BlockPos;

/** Pure navigation recomputation policy used by target and home movement. */
public final class GatheringNavigationCadence {
    public static final int RECOMPUTE_INTERVAL_TICKS = 10;

    private GatheringNavigationCadence() {
    }

    public static boolean shouldRecompute(
            BlockPos checkpointTarget,
            long lastRequestTick,
            BlockPos requestedTarget,
            long gameTime,
            boolean navigationDone,
            boolean stuck
    ) {
        if (checkpointTarget == null || !checkpointTarget.equals(requestedTarget)) {
            return true;
        }
        if (!navigationDone && !stuck) {
            return false;
        }
        return gameTime - lastRequestTick >= RECOMPUTE_INTERVAL_TICKS;
    }
}
