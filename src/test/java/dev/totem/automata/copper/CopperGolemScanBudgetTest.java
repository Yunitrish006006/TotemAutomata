package dev.totem.automata.copper;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CopperGolemScanBudgetTest {
    @Test void enforcesPerGolemAndServerCaps() {
        List<UUID> searchers = ids(12);
        CopperGolemScanBudget.Allocation allocation = CopperGolemScanBudget.allocate(searchers, 0);

        assertEquals(CopperGolemScanBudget.SERVER_LIMIT, allocation.totalGranted());
        assertEquals(8, allocation.grants().size());
        assertTrue(allocation.grants().values().stream()
                .allMatch(value -> value == CopperGolemScanBudget.PER_GOLEM_LIMIT));
    }

    @Test void rotatesAllocationSoLaterSearchersAreNotStarved() {
        List<UUID> searchers = ids(12);
        CopperGolemScanBudget.Allocation first = CopperGolemScanBudget.allocate(searchers, 0);
        CopperGolemScanBudget.Allocation second = CopperGolemScanBudget.allocate(
                searchers, first.nextStartIndex());

        assertEquals(0, first.grant(searchers.get(8)));
        assertEquals(CopperGolemScanBudget.PER_GOLEM_LIMIT, second.grant(searchers.get(8)));
        assertEquals(CopperGolemScanBudget.PER_GOLEM_LIMIT, second.grant(searchers.get(11)));
    }

    private static List<UUID> ids(int count) {
        List<UUID> ids = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            ids.add(new UUID(0, index + 1));
        }
        return List.copyOf(ids);
    }
}
