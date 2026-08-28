package dev.totem.automata.copper;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatheringNavigationCadenceTest {
    private static final BlockPos TARGET = new BlockPos(4, 5, 6);

    @Test void activeSameTargetPathIsReused() {
        assertFalse(GatheringNavigationCadence.shouldRecompute(
                TARGET, 20, TARGET, 200, false, false));
    }

    @Test void completedOrStuckPathWaitsForTenTickCooldown() {
        assertFalse(GatheringNavigationCadence.shouldRecompute(
                TARGET, 20, TARGET, 29, true, false));
        assertFalse(GatheringNavigationCadence.shouldRecompute(
                TARGET, 20, TARGET, 29, false, true));
        assertTrue(GatheringNavigationCadence.shouldRecompute(
                TARGET, 20, TARGET, 30, true, false));
        assertTrue(GatheringNavigationCadence.shouldRecompute(
                TARGET, 20, TARGET, 30, false, true));
    }

    @Test void targetChangeAllowsOneImmediateRecomputation() {
        assertTrue(GatheringNavigationCadence.shouldRecompute(
                TARGET, 29, TARGET.offset(1, 0, 0), 30, false, false));
    }
}
