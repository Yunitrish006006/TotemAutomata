package dev.totem.automata.client;

import dev.totem.automata.network.CopperGolemVisualizationPayload;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.SimpleGizmoCollector;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class CopperGolemVisualizationClientTest {
    private static final String OVERWORLD = "minecraft:overworld";

    @Test
    void completeGatheringAreaUsesOneDepthTestedCuboid() {
        SimpleGizmoCollector collector = collect(new CopperGolemVisualizationPayload.AreaEntry(
                OVERWORLD,
                true, -2, 80, -3,
                true, 2, 82, -3
        ), OVERWORLD);

        assertEquals(1, collector.getGizmos().size());
        assertFalse(collector.getGizmos().getFirst().isAlwaysOnTop());
    }

    @Test
    void incompleteCornersUseDepthTestedBlockOutlines() {
        SimpleGizmoCollector cornerA = collect(new CopperGolemVisualizationPayload.AreaEntry(
                OVERWORLD,
                true, 1, 2, 3,
                false, 0, 0, 0
        ), OVERWORLD);
        SimpleGizmoCollector cornerB = collect(new CopperGolemVisualizationPayload.AreaEntry(
                OVERWORLD,
                false, 0, 0, 0,
                true, 4, 5, 6
        ), OVERWORLD);

        assertEquals(1, cornerA.getGizmos().size());
        assertEquals(1, cornerB.getGizmos().size());
        assertFalse(cornerA.getGizmos().getFirst().isAlwaysOnTop());
        assertFalse(cornerB.getGizmos().getFirst().isAlwaysOnTop());
    }

    @Test
    void missingOrDifferentDimensionAreaSubmitsNothing() {
        assertEquals(0, collect(null, OVERWORLD).getGizmos().size());
        assertEquals(0, collect(new CopperGolemVisualizationPayload.AreaEntry(
                "minecraft:the_nether",
                true, 1, 2, 3,
                true, 4, 5, 6
        ), OVERWORLD).getGizmos().size());
    }

    private static SimpleGizmoCollector collect(
            CopperGolemVisualizationPayload.AreaEntry area,
            String dimension) {
        SimpleGizmoCollector collector = new SimpleGizmoCollector();
        try (var ignored = Gizmos.withCollector(collector)) {
            CopperGolemVisualizationClient.addGatheringAreaOutline(area, dimension);
        }
        return collector;
    }
}
