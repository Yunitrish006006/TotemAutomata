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
        return tick(tag, bounds, gameTime, GatheringScanCursor.DEFAULT_BUDGET, candidate);
    }

    public static GatheringScanCursor.Step tick(CompoundTag tag, GatheringScanCursor.Bounds bounds, long gameTime,
            int budget, Predicate<BlockPos> candidate) {
        return tick(tag, bounds, gameTime, budget, candidate, ignored -> true);
    }

    /**
     * Advances the lightweight cursor until the first cheap candidate and then
     * performs at most one expensive validation. A rejected expensive
     * candidate is not installed as the persisted target; its cursor position
     * is still consumed so the next budgeted tick resumes after it.
     */
    public static GatheringScanCursor.Step tick(
            CompoundTag tag,
            GatheringScanCursor.Bounds bounds,
            long gameTime,
            int budget,
            Predicate<BlockPos> cheapCandidate,
            Predicate<BlockPos> expensiveCandidate
    ) {
        Objects.requireNonNull(tag, "tag");
        Objects.requireNonNull(bounds, "bounds");
        Objects.requireNonNull(cheapCandidate, "cheapCandidate");
        Objects.requireNonNull(expensiveCandidate, "expensiveCandidate");
        if (budget <= 0) {
            return GatheringScanCursor.Step.idle(
                    GatheringRuntimeState.scanCursor(tag),
                    GatheringRuntimeState.scanActivity(tag),
                    GatheringRuntimeState.retryTick(tag)
            );
        }
        GatheringScanCursor.Step step = GatheringScanCursor.scan(bounds, GatheringRuntimeState.scanCursor(tag),
                GatheringRuntimeState.scanActivity(tag), GatheringRuntimeState.retryTick(tag), gameTime,
                Math.min(GatheringScanCursor.DEFAULT_BUDGET, Math.max(0, budget)), cheapCandidate);
        if (step.target().isPresent() && !expensiveCandidate.test(step.target().orElseThrow())) {
            step = GatheringScanCursor.Step.searching(step.nextCursor(), step.inspectedPositions());
        }
        GatheringRuntimeState.applyScanStep(tag, step);
        return step;
    }
}
