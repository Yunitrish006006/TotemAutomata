package dev.totem.automata.copper;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
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
}
