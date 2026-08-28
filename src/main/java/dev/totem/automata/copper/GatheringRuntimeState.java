package dev.totem.automata.copper;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

/** Legacy-compatible persisted scan/target state for the gathering runtime. */
public final class GatheringRuntimeState {
    public static final String TARGET_X = "deadrecall_gathering_target_x", TARGET_Y = "deadrecall_gathering_target_y", TARGET_Z = "deadrecall_gathering_target_z";
    public static final String SCAN_INDEX = "deadrecall_gathering_scan_index", RETRY_TICK = "deadrecall_gathering_retry_tick";
    private static final String NEAREST_RADIUS = "deadrecall_gathering_nearest_scan_radius", NEAREST_CURSOR = "deadrecall_gathering_nearest_scan_cursor";
    private static final String SKIPPED_TARGETS = "deadrecall_gathering_skipped_targets", WARMUP_INDEX = "deadrecall_gathering_llm_warmup_index";
    private GatheringRuntimeState() { }

    public static GatheringScanCursor.Activity scanActivity(CompoundTag tag) {
        return CopperGolemData.activity(tag) == CopperGolemActivity.BLOCKED_NO_VALID_TARGET
                ? GatheringScanCursor.Activity.BLOCKED_NO_VALID_TARGET : GatheringScanCursor.Activity.SEARCHING;
    }
    public static long scanCursor(CompoundTag tag) { return Math.max(0, tag.getLongOr(SCAN_INDEX, 0)); }
    public static long retryTick(CompoundTag tag) { return tag.getLongOr(RETRY_TICK, 0); }
    public static java.util.Optional<BlockPos> target(CompoundTag tag) {
        return tag.contains(TARGET_X) && tag.contains(TARGET_Y) && tag.contains(TARGET_Z)
                ? java.util.Optional.of(new BlockPos(tag.getIntOr(TARGET_X, 0), tag.getIntOr(TARGET_Y, 0), tag.getIntOr(TARGET_Z, 0))) : java.util.Optional.empty();
    }

    /** Persists the outcome of {@link GatheringScanCursor#scan} using the legacy runtime tags. */
    public static void applyScanStep(CompoundTag tag, GatheringScanCursor.Step step) {
        step.target().ifPresentOrElse(pos -> {
            tag.putLong(SCAN_INDEX, step.nextCursor()); clearNearestCursor(tag); clearTargetRetry(tag);
            tag.putInt(TARGET_X, pos.getX()); tag.putInt(TARGET_Y, pos.getY()); tag.putInt(TARGET_Z, pos.getZ());
            tag.putString(CopperGolemData.TAG_ACTIVITY, CopperGolemActivity.MOVING_TO_TARGET.id());
        }, () -> {
            clearNearestCursor(tag);
            if (step.activity() == GatheringScanCursor.Activity.BLOCKED_NO_VALID_TARGET) {
                tag.remove(SCAN_INDEX); tag.remove(SKIPPED_TARGETS); clearTargetTags(tag);
                tag.putLong(RETRY_TICK, step.retryTick()); tag.putString(CopperGolemData.TAG_ACTIVITY, CopperGolemActivity.BLOCKED_NO_VALID_TARGET.id());
            } else {
                tag.putLong(SCAN_INDEX, step.nextCursor()); tag.remove(RETRY_TICK);
                tag.putString(CopperGolemData.TAG_ACTIVITY, CopperGolemActivity.SEARCHING.id());
            }
        });
    }

    public static void resetSearch(CompoundTag tag, boolean clearSkippedTargets) {
        clearTargetTags(tag); tag.remove(SCAN_INDEX); clearNearestCursor(tag); if (clearSkippedTargets) tag.remove(SKIPPED_TARGETS);
        tag.remove(WARMUP_INDEX); tag.remove(RETRY_TICK); tag.remove(CopperGolemData.TAG_ACTIVITY);
    }
    /** Writes an activity only when it is an actual state transition. */
    public static boolean setActivity(CompoundTag tag, CopperGolemActivity activity) {
        if (CopperGolemData.activity(tag) == activity
                && tag.getStringOr(CopperGolemData.TAG_ACTIVITY, "").equals(activity.id())) {
            return false;
        }
        tag.putString(CopperGolemData.TAG_ACTIVITY, activity.id());
        return true;
    }
    /** Clears a rejected target while preserving the resumable cursor. */
    public static void deferTarget(CompoundTag tag, long retryTick) {
        clearTargetTags(tag);
        tag.putLong(RETRY_TICK, retryTick);
        setActivity(tag, CopperGolemActivity.BLOCKED_NO_VALID_TARGET);
    }
    public static void clearTarget(CompoundTag tag) { clearTargetTags(tag); tag.remove(CopperGolemData.TAG_ACTIVITY); }
    private static void clearTargetRetry(CompoundTag tag) { tag.remove(RETRY_TICK); }
    private static void clearTargetTags(CompoundTag tag) { tag.remove(TARGET_X); tag.remove(TARGET_Y); tag.remove(TARGET_Z); }
    private static void clearNearestCursor(CompoundTag tag) { tag.remove(NEAREST_RADIUS); tag.remove(NEAREST_CURSOR); }
}
