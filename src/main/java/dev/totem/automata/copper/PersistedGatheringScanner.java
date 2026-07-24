package dev.totem.automata.copper;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * Per-tick persisted gathering scan orchestration.
 *
 * <p>The live world adapter supplies the candidate predicate; this class
 * keeps scan budget, retry behavior, and legacy NBT updates together.</p>
 */
public final class PersistedGatheringScanner {
    private PersistedGatheringScanner() { }
    public static GatheringScanCursor.Step tick(CompoundTag tag, GatheringScanCursor.Bounds bounds, long gameTime,
            Predicate<BlockPos> candidate) {
        Objects.requireNonNull(tag, "tag"); Objects.requireNonNull(bounds, "bounds"); Objects.requireNonNull(candidate, "candidate");
        GatheringScanCursor.Step step = GatheringScanCursor.scan(bounds, GatheringRuntimeState.scanCursor(tag),
                GatheringRuntimeState.scanActivity(tag), GatheringRuntimeState.retryTick(tag), gameTime,
                GatheringScanCursor.DEFAULT_BUDGET, candidate);
        GatheringRuntimeState.applyScanStep(tag, step);
        return step;
    }
}
