package dev.totem.automata.copper;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Discovers, tracks and ticks managed Copper Golems without assuming handler internals. */
public final class CopperGolemController {
    private static final int PRUNE_BINDINGS_INTERVAL_TICKS = 20;

    private final Map<UUID, ResourceKey<Level>> tracked = new ConcurrentHashMap<>();
    private int pruneBindingsTicker;
    private int scanRotationIndex;
    private Diagnostics diagnostics = Diagnostics.EMPTY;

    public void tick(MinecraftServer server, CopperGolemBehavior behavior) {
        boolean shouldPruneBindings = ++pruneBindingsTicker >= PRUNE_BINDINGS_INTERVAL_TICKS;
        if (shouldPruneBindings) pruneBindingsTicker = 0;
        tickTracked(server, behavior, shouldPruneBindings);
    }

    public void track(CopperGolem golem) {
        if (!golem.level().isClientSide() && !golem.isRemoved()) {
            tracked.put(golem.getUUID(), golem.level().dimension());
        }
    }

    public void untrack(CopperGolem golem) {
        tracked.remove(golem.getUUID());
        GatheringNavigation.forget(golem.getUUID());
    }

    public void clear() {
        tracked.clear();
        scanRotationIndex = 0;
        diagnostics = Diagnostics.EMPTY;
        GatheringNavigation.clearTransientState();
    }

    private void tickTracked(MinecraftServer server, CopperGolemBehavior behavior, boolean shouldPruneBindings) {
        List<LoadedGolem> loaded = new ArrayList<>();
        for (Map.Entry<UUID, ResourceKey<Level>> entry : tracked.entrySet()) {
            ServerLevel level = server.getLevel(entry.getValue());
            if (level == null) {
                tracked.remove(entry.getKey());
                GatheringNavigation.forget(entry.getKey());
                continue;
            }
            if (!(level.getEntity(entry.getKey()) instanceof CopperGolem golem) || golem.isRemoved() || !golem.isAlive()) {
                tracked.remove(entry.getKey());
                GatheringNavigation.forget(entry.getKey());
                continue;
            }
            loaded.add(new LoadedGolem(level, golem, behavior.scheduling(level, golem)));
        }
        loaded.sort(Comparator.comparing(value -> value.golem().getUUID()));

        List<UUID> searchers = loaded.stream()
                .filter(value -> value.scheduling().shouldTick() && value.scheduling().needsScanBudget())
                .map(value -> value.golem().getUUID())
                .toList();
        CopperGolemScanBudget.Allocation allocation = CopperGolemScanBudget.allocate(searchers, scanRotationIndex);
        scanRotationIndex = allocation.nextStartIndex();

        int ticked = 0;
        int inspected = 0;
        for (LoadedGolem value : loaded) {
            if (!value.scheduling().shouldTick()) {
                continue;
            }
            inspected += behavior.tickScheduled(
                    server,
                    value.level(),
                    value.golem(),
                    shouldPruneBindings,
                    allocation.grant(value.golem().getUUID())
            ).scanPositionsInspected();
            ticked++;
        }
        diagnostics = new Diagnostics(tracked.size(), ticked, searchers.size(), inspected);
    }

    public Diagnostics diagnostics() {
        return diagnostics;
    }

    boolean isTracked(UUID golemId) {
        return tracked.containsKey(golemId);
    }

    private record LoadedGolem(ServerLevel level, CopperGolem golem, CopperGolemBehavior.Scheduling scheduling) {
    }

    public record Diagnostics(int tracked, int ticked, int searchers, int scanPositionsInspected) {
        private static final Diagnostics EMPTY = new Diagnostics(0, 0, 0, 0);
    }
}
