package dev.totem.automata.copper;

/** Pure blocked-route retry schedule; inventory hashes are checked only when due. */
public final class SortingBlockedBackoff {
    public static final int INITIAL_DELAY_TICKS = 10;
    public static final int MAX_DELAY_TICKS = 200;

    private SortingBlockedBackoff() {
    }

    public static int normalizeDelay(int delay) {
        return delay < INITIAL_DELAY_TICKS
                ? INITIAL_DELAY_TICKS
                : Math.min(delay, MAX_DELAY_TICKS);
    }

    public static int nextDelay(int currentDelay) {
        int normalized = normalizeDelay(currentDelay);
        return Math.min(MAX_DELAY_TICKS, normalized * 2);
    }

    public static boolean due(long gameTick, long nextRetryTick) {
        return nextRetryTick <= 0 || gameTick >= nextRetryTick;
    }
}
