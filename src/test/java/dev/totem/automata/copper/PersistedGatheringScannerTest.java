package dev.totem.automata.copper;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class PersistedGatheringScannerTest {
    @Test void drivesCursorAndPersistsTheFirstAcceptedWorldCandidate() {
        CompoundTag tag = new CompoundTag();
        var bounds = new GatheringScanCursor.Bounds(0, 0, 0, 1, 0, 1);
        var step = PersistedGatheringScanner.tick(tag, bounds, 10, pos -> pos.equals(new BlockPos(1, 0, 0)));
        assertEquals(new BlockPos(1, 0, 0), step.target().orElseThrow());
        assertEquals(new BlockPos(1, 0, 0), GatheringRuntimeState.target(tag).orElseThrow());
        assertEquals(CopperGolemActivity.MOVING_TO_TARGET, CopperGolemData.activity(tag));
    }

    @Test void capsLightweightWorkAndRunsOnlyOneExpensiveValidation() {
        CompoundTag tag = new CompoundTag();
        var bounds = new GatheringScanCursor.Bounds(0, 0, 0, 63, 0, 0);
        AtomicInteger cheapChecks = new AtomicInteger();
        AtomicInteger expensiveChecks = new AtomicInteger();

        var rejected = PersistedGatheringScanner.tick(
                tag,
                bounds,
                10,
                100,
                pos -> {
                    cheapChecks.incrementAndGet();
                    return pos.getX() >= 7;
                },
                pos -> {
                    expensiveChecks.incrementAndGet();
                    return false;
                }
        );

        assertTrue(rejected.target().isEmpty());
        assertEquals(8, cheapChecks.get(), "cheap scan should stop at the first candidate");
        assertEquals(1, expensiveChecks.get(), "only one candidate may enter expensive validation");
        assertEquals(8, GatheringRuntimeState.scanCursor(tag));

        cheapChecks.set(0);
        PersistedGatheringScanner.tick(tag, bounds, 11, 100, pos -> {
            cheapChecks.incrementAndGet();
            return false;
        }, pos -> {
            fail("a tick without a cheap candidate must not perform expensive validation");
            return false;
        });
        assertEquals(CopperGolemScanBudget.PER_GOLEM_LIMIT, cheapChecks.get());
        assertEquals(40, GatheringRuntimeState.scanCursor(tag));
    }

    @Test void zeroBudgetDoesNotInvokePredicatesOrMutatePersistedState() {
        CompoundTag tag = new CompoundTag();
        tag.putLong(GatheringRuntimeState.SCAN_INDEX, 9);
        CompoundTag before = tag.copy();
        var bounds = new GatheringScanCursor.Bounds(0, 0, 0, 31, 0, 0);

        GatheringScanCursor.Step idle = PersistedGatheringScanner.tick(
                tag,
                bounds,
                10,
                0,
                pos -> {
                    fail("zero budget invoked lightweight discovery");
                    return true;
                },
                pos -> {
                    fail("zero budget invoked expensive validation");
                    return true;
                }
        );

        assertEquals(0, idle.inspectedPositions());
        assertEquals(before, tag);
    }
}
