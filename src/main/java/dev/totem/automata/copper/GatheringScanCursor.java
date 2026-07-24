package dev.totem.automata.copper;

import net.minecraft.core.BlockPos;

import java.util.Optional;
import java.util.function.Predicate;

/** Bounded, resumable top-down gathering scan independent of world validation. */
public final class GatheringScanCursor {
    public static final int DEFAULT_BUDGET = 512;
    public static final int RETRY_TICKS = 100;
    private GatheringScanCursor() { }

    public static Step scan(Bounds bounds, long storedCursor, Activity activity, long retryTick, long gameTime,
            int budget, Predicate<BlockPos> candidate) {
        if (activity == Activity.BLOCKED_NO_VALID_TARGET && gameTime < retryTick)
            return Step.waiting(storedCursor, retryTick);
        long cursor = storedCursor < 0 || storedCursor > bounds.volume() ? 0 : storedCursor;
        int remaining = (int) Math.min(Math.max(0, budget), bounds.volume());
        while (cursor < bounds.volume() && remaining-- > 0) {
            BlockPos pos = bounds.topDownPositionAt(cursor++);
            if (candidate.test(pos)) return Step.target(pos, cursor);
        }
        if (cursor >= bounds.volume()) return Step.blocked(gameTime + RETRY_TICKS);
        return Step.searching(cursor);
    }

    public record Bounds(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        public Bounds { if (minX > maxX || minY > maxY || minZ > maxZ) throw new IllegalArgumentException("inverted gathering bounds"); }
        public long volume() { return ((long) maxX - minX + 1) * ((long) maxY - minY + 1) * ((long) maxZ - minZ + 1); }
        public boolean contains(BlockPos pos) { return pos.getX() >= minX && pos.getX() <= maxX && pos.getY() >= minY && pos.getY() <= maxY && pos.getZ() >= minZ && pos.getZ() <= maxZ; }
        public BlockPos topDownPositionAt(long index) {
            if (index < 0 || index >= volume()) throw new IndexOutOfBoundsException("gathering scan index " + index);
            long sizeX = (long) maxX - minX + 1, layerSize = sizeX * ((long) maxZ - minZ + 1), layerIndex = index % layerSize;
            return new BlockPos(minX + (int) (layerIndex % sizeX), maxY - (int) (index / layerSize), minZ + (int) (layerIndex / sizeX));
        }
    }
    public enum Activity { SEARCHING, BLOCKED_NO_VALID_TARGET }
    public record Step(Optional<BlockPos> target, long nextCursor, Activity activity, long retryTick) {
        static Step target(BlockPos pos, long cursor) { return new Step(Optional.of(pos), cursor, Activity.SEARCHING, 0); }
        static Step searching(long cursor) { return new Step(Optional.empty(), cursor, Activity.SEARCHING, 0); }
        static Step blocked(long retryTick) { return new Step(Optional.empty(), 0, Activity.BLOCKED_NO_VALID_TARGET, retryTick); }
        static Step waiting(long cursor, long retryTick) { return new Step(Optional.empty(), cursor, Activity.BLOCKED_NO_VALID_TARGET, retryTick); }
    }
}
