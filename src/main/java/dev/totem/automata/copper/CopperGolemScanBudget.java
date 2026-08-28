package dev.totem.automata.copper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Pure, deterministic allocator for the shared gathering scan budget. */
public final class CopperGolemScanBudget {
    public static final int SERVER_LIMIT = 256;
    public static final int PER_GOLEM_LIMIT = 32;

    private CopperGolemScanBudget() {
    }

    public static Allocation allocate(List<UUID> searchers, int startIndex) {
        if (searchers.isEmpty()) {
            return new Allocation(Map.of(), 0, 0);
        }
        int start = Math.floorMod(startIndex, searchers.size());
        int remaining = SERVER_LIMIT;
        int served = 0;
        Map<UUID, Integer> grants = new LinkedHashMap<>();
        for (int offset = 0; offset < searchers.size() && remaining > 0; offset++) {
            UUID searcher = searchers.get((start + offset) % searchers.size());
            int grant = Math.min(PER_GOLEM_LIMIT, remaining);
            grants.put(searcher, grant);
            remaining -= grant;
            served++;
        }
        return new Allocation(Map.copyOf(grants), (start + served) % searchers.size(), SERVER_LIMIT - remaining);
    }

    public record Allocation(Map<UUID, Integer> grants, int nextStartIndex, int totalGranted) {
        public Allocation {
            grants = Map.copyOf(grants);
        }

        public int grant(UUID golemId) {
            return grants.getOrDefault(golemId, 0);
        }
    }
}
