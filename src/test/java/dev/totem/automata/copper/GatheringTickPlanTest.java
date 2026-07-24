package dev.totem.automata.copper;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class GatheringTickPlanTest {
    @Test void blocksBeforeTryingToScanWithoutAreaOrHome() {
        assertEquals(GatheringTickPlan.Action.BLOCKED_NO_AREA, GatheringTickPlan.decide(false, false, false, CopperGolemActivity.SEARCHING));
        assertEquals(GatheringTickPlan.Action.BLOCKED_NO_HOME, GatheringTickPlan.decide(true, false, false, CopperGolemActivity.SEARCHING));
    }
    @Test void returnsHomeForStorageAndTerminalScanStates() {
        assertEquals(GatheringTickPlan.Action.DEPOSIT, GatheringTickPlan.decide(true, true, true, CopperGolemActivity.SEARCHING));
        assertEquals(GatheringTickPlan.Action.DEPOSIT, GatheringTickPlan.decide(true, true, false, CopperGolemActivity.BLOCKED_NO_VALID_TARGET));
        assertEquals(GatheringTickPlan.Action.SCAN, GatheringTickPlan.decide(true, true, false, CopperGolemActivity.SEARCHING));
    }
}
