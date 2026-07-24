package dev.totem.automata.copper;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

class GatheringRuntimeStateTest {
    @Test void writesFoundTargetAndMovingActivity() {
        CompoundTag tag = new CompoundTag();
        GatheringRuntimeState.applyScanStep(tag, new GatheringScanCursor.Step(Optional.of(new BlockPos(2, 70, 3)), 12, GatheringScanCursor.Activity.SEARCHING, 0));
        assertEquals(new BlockPos(2, 70, 3), GatheringRuntimeState.target(tag).orElseThrow());
        assertEquals(12, GatheringRuntimeState.scanCursor(tag));
        assertEquals(CopperGolemActivity.MOVING_TO_TARGET, CopperGolemData.activity(tag));
    }
    @Test void writesBlockedRetryAndClearsCompletedSearchState() {
        CompoundTag tag = new CompoundTag(); tag.putLong(GatheringRuntimeState.SCAN_INDEX, 5);
        GatheringRuntimeState.applyScanStep(tag, new GatheringScanCursor.Step(Optional.empty(), 0, GatheringScanCursor.Activity.BLOCKED_NO_VALID_TARGET, 120));
        assertFalse(tag.contains(GatheringRuntimeState.SCAN_INDEX)); assertEquals(120, GatheringRuntimeState.retryTick(tag));
        assertEquals(CopperGolemActivity.BLOCKED_NO_VALID_TARGET, CopperGolemData.activity(tag));
    }
}
