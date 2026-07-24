package dev.totem.automata.copper;

/** Pure top-level gathering tick decision before world-specific execution. */
public final class GatheringTickPlan {
    private GatheringTickPlan() { }
    public static Action decide(boolean hasCompleteArea, boolean hasHome, boolean storageFull, CopperGolemActivity activity) {
        if (!hasCompleteArea) return Action.BLOCKED_NO_AREA;
        if (!hasHome) return Action.BLOCKED_NO_HOME;
        if (storageFull || activity == CopperGolemActivity.RETURNING_HOME || activity == CopperGolemActivity.DEPOSITING || activity == CopperGolemActivity.BLOCKED_NO_VALID_TARGET) return Action.DEPOSIT;
        return Action.SCAN;
    }
    public enum Action { SCAN, DEPOSIT, BLOCKED_NO_AREA, BLOCKED_NO_HOME }
}
