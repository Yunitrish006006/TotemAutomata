package dev.totem.automata.copper;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Legacy timing guards for Wrench entity/block and gathering-target gestures. */
public final class CopperWrenchInteractionDebounce {
    private static final int ENTITY_TO_BLOCK_TICKS = 2, GATHERING_TARGET_TICKS = 8;
    private final Map<EntityUseKey, Long> entityUses = new HashMap<>();
    private final Map<GatheringClickKey, Long> gatheringClicks = new HashMap<>();

    public void recordEntityUse(UUID player, InteractionHand hand, boolean client, long gameTime) {
        prune(entityUses, gameTime, ENTITY_TO_BLOCK_TICKS); entityUses.put(new EntityUseKey(player, hand, client), gameTime);
    }
    public boolean consumeEntityToBlockSuppression(UUID player, InteractionHand hand, boolean client, long gameTime) {
        Long then = entityUses.remove(new EntityUseKey(player, hand, client));
        return then != null && !expired(gameTime, then, ENTITY_TO_BLOCK_TICKS);
    }
    public boolean isGatheringTargetDuplicate(UUID player, UUID golem, ResourceKey<Level> dimension, BlockPos pos, String blockId, long gameTime) {
        prune(gatheringClicks, gameTime, GATHERING_TARGET_TICKS);
        Long previous = gatheringClicks.put(new GatheringClickKey(player, golem, dimension, pos.immutable(), blockId), gameTime);
        return previous != null && gameTime - previous <= GATHERING_TARGET_TICKS;
    }
    private static boolean expired(long now, long then, int ticks) { long age = now - then; return age < 0 || age > ticks; }
    private static <K> void prune(Map<K, Long> values, long now, int ticks) { if (values.size() > 256) values.entrySet().removeIf(entry -> expired(now, entry.getValue(), ticks)); }
    private record EntityUseKey(UUID player, InteractionHand hand, boolean client) { }
    private record GatheringClickKey(UUID player, UUID golem, ResourceKey<Level> dimension, BlockPos pos, String blockId) { }
}
