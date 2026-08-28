package dev.totem.automata.copper;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SortingBlockedBackoffTest {
    @Test void doublesFromTenTicksAndCapsAtTwoHundred() {
        assertEquals(20, SortingBlockedBackoff.nextDelay(10));
        assertEquals(40, SortingBlockedBackoff.nextDelay(20));
        assertEquals(80, SortingBlockedBackoff.nextDelay(40));
        assertEquals(160, SortingBlockedBackoff.nextDelay(80));
        assertEquals(200, SortingBlockedBackoff.nextDelay(160));
        assertEquals(200, SortingBlockedBackoff.nextDelay(200));
    }

    @Test void retryBecomesDueOnlyAtTheScheduledTick() {
        assertFalse(SortingBlockedBackoff.due(109, 110));
        assertTrue(SortingBlockedBackoff.due(110, 110));
        assertTrue(SortingBlockedBackoff.due(50, 0));
    }
}
