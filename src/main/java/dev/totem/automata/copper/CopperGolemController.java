package dev.totem.automata.copper;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Discovers, tracks and ticks managed Copper Golems without assuming handler internals. */
public final class CopperGolemController {
    private static final int PRUNE_BINDINGS_INTERVAL_TICKS = 20;
    private static final int DISCOVERY_INTERVAL_TICKS = 20;

    private final Map<UUID, ResourceKey<Level>> tracked = new ConcurrentHashMap<>();
    private int pruneBindingsTicker;
    private int discoveryTicker = DISCOVERY_INTERVAL_TICKS - 1;

    public void tick(MinecraftServer server, CopperGolemBehavior behavior) {
        boolean shouldPruneBindings = ++pruneBindingsTicker >= PRUNE_BINDINGS_INTERVAL_TICKS;
        if (shouldPruneBindings) pruneBindingsTicker = 0;
        if (++discoveryTicker >= DISCOVERY_INTERVAL_TICKS) {
            discoveryTicker = 0;
            discover(server, behavior);
        }
        tickTracked(server, behavior, shouldPruneBindings);
    }

    public void track(CopperGolem golem) {
        if (!golem.level().isClientSide() && !golem.isRemoved()) {
            tracked.put(golem.getUUID(), golem.level().dimension());
        }
    }

    public void untrack(CopperGolem golem) {
        tracked.remove(golem.getUUID());
    }

    private void discover(MinecraftServer server, CopperGolemBehavior behavior) {
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof CopperGolem golem && behavior.shouldTrack(golem)) track(golem);
            }
        }
    }

    private void tickTracked(MinecraftServer server, CopperGolemBehavior behavior, boolean shouldPruneBindings) {
        for (Map.Entry<UUID, ResourceKey<Level>> entry : new ArrayList<>(tracked.entrySet())) {
            ServerLevel level = server.getLevel(entry.getValue());
            if (level == null) {
                tracked.remove(entry.getKey());
                continue;
            }
            Entity entity = level.getEntity(entry.getKey());
            if (!(entity instanceof CopperGolem golem) || golem.isRemoved() || !golem.isAlive()) {
                tracked.remove(entry.getKey());
                continue;
            }
            behavior.tick(server, level, golem, shouldPruneBindings);
            if (!behavior.shouldTrack(golem)) tracked.remove(entry.getKey());
        }
    }
}
