package dev.totem.automata.copper;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

class GatheringRuntimeStateTest {
    @Test void writesFoundTargetAndMovingActivity() {
        CompoundTag tag = new CompoundTag();
        GatheringRuntimeState.applyScanStep(tag, new GatheringScanCursor.Step(Optional.of(new BlockPos(2, 70, 3)), 12, GatheringScanCursor.Activity.SEARCHING, 0, 0));
        assertEquals(new BlockPos(2, 70, 3), GatheringRuntimeState.target(tag).orElseThrow());
        assertEquals(12, GatheringRuntimeState.scanCursor(tag));
        assertEquals(CopperGolemActivity.MOVING_TO_TARGET, CopperGolemData.activity(tag));
    }
    @Test void writesBlockedRetryAndClearsCompletedSearchState() {
        CompoundTag tag = new CompoundTag(); tag.putLong(GatheringRuntimeState.SCAN_INDEX, 5);
        GatheringRuntimeState.applyScanStep(tag, new GatheringScanCursor.Step(Optional.empty(), 0, GatheringScanCursor.Activity.BLOCKED_NO_VALID_TARGET, 120, 0));
        assertFalse(tag.contains(GatheringRuntimeState.SCAN_INDEX)); assertEquals(120, GatheringRuntimeState.retryTick(tag));
        assertEquals(CopperGolemActivity.BLOCKED_NO_VALID_TARGET, CopperGolemData.activity(tag));
    }

    @Test void rejectedTargetBacksOffWithoutLosingCursor() {
        CompoundTag tag = new CompoundTag();
        tag.putLong(GatheringRuntimeState.SCAN_INDEX, 42);
        tag.putInt(GatheringRuntimeState.TARGET_X, 2);
        tag.putInt(GatheringRuntimeState.TARGET_Y, 70);
        tag.putInt(GatheringRuntimeState.TARGET_Z, 3);

        GatheringRuntimeState.deferTarget(tag, 150);

        assertTrue(GatheringRuntimeState.target(tag).isEmpty());
        assertEquals(42, GatheringRuntimeState.scanCursor(tag));
        assertEquals(150, GatheringRuntimeState.retryTick(tag));
        assertEquals(CopperGolemActivity.BLOCKED_NO_VALID_TARGET, CopperGolemData.activity(tag));
    }

    @Test void unchangedActivityDoesNotMutateTheTag() {
        CompoundTag tag = new CompoundTag();
        assertTrue(GatheringRuntimeState.setActivity(tag, CopperGolemActivity.SEARCHING));
        CompoundTag afterTransition = tag.copy();
        assertFalse(GatheringRuntimeState.setActivity(tag, CopperGolemActivity.SEARCHING));
        assertEquals(afterTransition, tag);
    }
}
