package dev.totem.automata.copper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Legacy-compatible persisted gathering-area and manual-target configuration. */
public final class GatheringConfiguration {
    public static final String AREA_DIMENSION = "deadrecall_gathering_area_dim";
    public static final String CORNER_A_X = "deadrecall_gathering_corner_a_x", CORNER_A_Y = "deadrecall_gathering_corner_a_y", CORNER_A_Z = "deadrecall_gathering_corner_a_z";
    public static final String CORNER_B_X = "deadrecall_gathering_corner_b_x", CORNER_B_Y = "deadrecall_gathering_corner_b_y", CORNER_B_Z = "deadrecall_gathering_corner_b_z";
    public static final String MANUAL_TARGETS = "deadrecall_gathering_manual_targets";
    public static final int MAX_AXIS_LENGTH = 64;
    public static final long MAX_VOLUME = 262_144L;
    public static final int MAX_MANUAL_TARGETS = 64;

    private GatheringConfiguration() { }

    public static Optional<Area> readArea(CompoundTag tag) {
        Identifier id = Identifier.tryParse(tag.getStringOr(AREA_DIMENSION, ""));
        if (id == null) return Optional.empty();
        Optional<BlockPos> a = readPos(tag, CORNER_A_X, CORNER_A_Y, CORNER_A_Z);
        Optional<BlockPos> b = readPos(tag, CORNER_B_X, CORNER_B_Y, CORNER_B_Z);
        return a.isEmpty() && b.isEmpty() ? Optional.empty() : Optional.of(new Area(ResourceKey.create(Registries.DIMENSION, id), a, b));
    }

    /** Writes one corner, clearing an old cross-dimension area and rejecting oversize completed areas. */
    public static CornerUpdate setCorner(CompoundTag tag, ResourceKey<net.minecraft.world.level.Level> dimension, BlockPos pos, boolean cornerB) {
        Area existing = readArea(tag).orElse(null);
        if (existing != null && !existing.dimension().equals(dimension)) clearArea(tag);
        Optional<BlockPos> a = cornerB ? readPos(tag, CORNER_A_X, CORNER_A_Y, CORNER_A_Z) : Optional.of(pos);
        Optional<BlockPos> b = cornerB ? Optional.of(pos) : readPos(tag, CORNER_B_X, CORNER_B_Y, CORNER_B_Z);
        if (a.isPresent() && b.isPresent() && !withinLimits(a.get(), b.get())) return CornerUpdate.TOO_LARGE;
        tag.putString(AREA_DIMENSION, dimension.identifier().toString());
        if (cornerB) writePos(tag, pos, CORNER_B_X, CORNER_B_Y, CORNER_B_Z); else writePos(tag, pos, CORNER_A_X, CORNER_A_Y, CORNER_A_Z);
        return CornerUpdate.UPDATED;
    }

    public static void clearArea(CompoundTag tag) {
        tag.remove(AREA_DIMENSION); tag.remove(CORNER_A_X); tag.remove(CORNER_A_Y); tag.remove(CORNER_A_Z);
        tag.remove(CORNER_B_X); tag.remove(CORNER_B_Y); tag.remove(CORNER_B_Z);
    }

    public static List<String> manualTargets(CompoundTag tag) {
        return tag.getList(MANUAL_TARGETS).map(list -> list.stream().map(value -> value.asString().orElse(""))
                .filter(value -> !value.isBlank()).limit(MAX_MANUAL_TARGETS).toList()).orElse(List.of());
    }

    /** Adds/removes a manual target, evicting the oldest target at the legacy limit. */
    public static boolean toggleManualTarget(CompoundTag tag, String blockId) {
        if (blockId == null || Identifier.tryParse(blockId) == null) return false;
        List<String> targets = new ArrayList<>(manualTargets(tag));
        if (targets.remove(blockId)) { writeTargets(tag, targets); return false; }
        if (targets.size() >= MAX_MANUAL_TARGETS) targets.removeFirst();
        targets.add(blockId); writeTargets(tag, targets); return true;
    }

    public static boolean removeManualTarget(CompoundTag tag, String blockId) {
        List<String> targets = new ArrayList<>(manualTargets(tag));
        if (!targets.remove(blockId)) return false;
        writeTargets(tag, targets); return true;
    }

    public static boolean withinLimits(BlockPos a, BlockPos b) {
        return withinLimits(Math.abs((long) a.getX() - b.getX()) + 1, Math.abs((long) a.getY() - b.getY()) + 1, Math.abs((long) a.getZ() - b.getZ()) + 1);
    }
    public static Optional<GatheringScanCursor.Bounds> scanBounds(CompoundTag tag, ResourceKey<net.minecraft.world.level.Level> currentDimension) {
        return readArea(tag).filter(area -> area.dimension().equals(currentDimension) && area.complete()).flatMap(area -> {
            BlockPos a = area.cornerA().orElseThrow(), b = area.cornerB().orElseThrow();
            if (!withinLimits(a, b)) return Optional.empty();
            return Optional.of(new GatheringScanCursor.Bounds(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()), Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY()), Math.max(a.getZ(), b.getZ())));
        });
    }
    public static boolean withinLimits(long x, long y, long z) { return x <= MAX_AXIS_LENGTH && y <= MAX_AXIS_LENGTH && z <= MAX_AXIS_LENGTH && x * y * z <= MAX_VOLUME; }

    private static Optional<BlockPos> readPos(CompoundTag tag, String x, String y, String z) {
        return tag.contains(x) && tag.contains(y) && tag.contains(z) ? Optional.of(new BlockPos(tag.getIntOr(x, 0), tag.getIntOr(y, 0), tag.getIntOr(z, 0))) : Optional.empty();
    }
    private static void writePos(CompoundTag tag, BlockPos pos, String x, String y, String z) { tag.putInt(x, pos.getX()); tag.putInt(y, pos.getY()); tag.putInt(z, pos.getZ()); }
    private static void writeTargets(CompoundTag tag, List<String> targets) {
        ListTag values = new ListTag();
        targets.stream().filter(value -> value != null && !value.isBlank()).limit(MAX_MANUAL_TARGETS)
                .forEach(value -> values.add(StringTag.valueOf(value)));
        if (values.isEmpty()) tag.remove(MANUAL_TARGETS); else tag.put(MANUAL_TARGETS, values);
    }

    public enum CornerUpdate { UPDATED, TOO_LARGE }
    public record Area(ResourceKey<net.minecraft.world.level.Level> dimension, Optional<BlockPos> cornerA, Optional<BlockPos> cornerB) {
        public boolean complete() { return cornerA.isPresent() && cornerB.isPresent(); }
    }
}
