package dev.totem.automata.copper;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GatheringScanCursorTest {
    private static final GatheringScanCursor.Bounds BOUNDS = new GatheringScanCursor.Bounds(0, 10, 0, 1, 11, 1);
    @Test void scansTopDownThenAcrossEachLayer() {
        assertEquals(new BlockPos(0, 11, 0), BOUNDS.topDownPositionAt(0));
        assertEquals(new BlockPos(1, 11, 0), BOUNDS.topDownPositionAt(1));
        assertEquals(new BlockPos(0, 11, 1), BOUNDS.topDownPositionAt(2));
        assertEquals(new BlockPos(0, 10, 0), BOUNDS.topDownPositionAt(4));
    }
    @Test void resumesAtBudgetAndPersistsCursorAfterTarget() {
        var searching = GatheringScanCursor.scan(BOUNDS, 0, GatheringScanCursor.Activity.SEARCHING, 0, 20, 2, pos -> false);
        assertEquals(GatheringScanCursor.Activity.SEARCHING, searching.activity()); assertEquals(2, searching.nextCursor());
        var found = GatheringScanCursor.scan(BOUNDS, searching.nextCursor(), searching.activity(), 0, 20, 4, pos -> pos.equals(new BlockPos(0, 10, 0)));
        assertEquals(new BlockPos(0, 10, 0), found.target().orElseThrow()); assertEquals(5, found.nextCursor());
    }
    @Test void blocksAndWaitsAfterCompleteMiss() {
        var blocked = GatheringScanCursor.scan(BOUNDS, 0, GatheringScanCursor.Activity.SEARCHING, 0, 20, 99, pos -> false);
        assertEquals(GatheringScanCursor.Activity.BLOCKED_NO_VALID_TARGET, blocked.activity()); assertEquals(120, blocked.retryTick());
        var waiting = GatheringScanCursor.scan(BOUNDS, 0, blocked.activity(), blocked.retryTick(), 30, 99, pos -> true);
        assertTrue(waiting.target().isEmpty()); assertEquals(120, waiting.retryTick());
    }
}
