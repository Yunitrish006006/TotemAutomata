package dev.totem.automata.client;

import dev.totem.automata.network.CopperGolemVisualizationPayload;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.LineGizmo;
import net.minecraft.gizmos.SimpleGizmoCollector;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class CopperGolemVisualizationClientTest {
    private static final String OVERWORLD = "minecraft:overworld";
    private static final Vec3 GOLEM_CENTER = new Vec3(0.5D, 80.9D, -3.5D);

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

    @Test
    void sortingLinksUseSolidDepthTestedAvailabilityColours() {
        CopperGolemVisualizationPayload.PosEntry source = pos(-3, 80, -4, true);
        CopperGolemVisualizationPayload.PosEntry available = pos(3, 80, -4, true);
        CopperGolemVisualizationPayload.PosEntry unavailable = pos(3, 82, -4, false);
        SimpleGizmoCollector collector = collectLinks(
                source,
                List.of(available, unavailable),
                "sorting",
                OVERWORLD
        );

        assertEquals(3, collector.getGizmos().size());
        assertLink(collector, 0, source, 0xFFFFB74D);
        assertLink(collector, 1, available, 0xFF66BB6A);
        assertLink(collector, 2, unavailable, 0xFFEF5350);
    }

    @Test
    void gatheringModeKeepsOnlyTheSourceContainerLink() {
        CopperGolemVisualizationPayload.PosEntry source = pos(-3, 80, -4, true);
        SimpleGizmoCollector collector = collectLinks(
                source,
                List.of(pos(3, 80, -4, true)),
                "gathering",
                OVERWORLD
        );

        assertEquals(1, collector.getGizmos().size());
        assertLink(collector, 0, source, 0xFFFFB74D);
    }

    @Test
    void missingAndDifferentDimensionContainersSubmitNoLinks() {
        CopperGolemVisualizationPayload.PosEntry nether = new CopperGolemVisualizationPayload.PosEntry(
                "minecraft:the_nether",
                1, 2, 3,
                true
        );

        assertEquals(0, collectLinks(null, List.of(), "sorting", OVERWORLD).getGizmos().size());
        assertEquals(0, collectLinks(nether, List.of(nether), "sorting", OVERWORLD).getGizmos().size());
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

    private static SimpleGizmoCollector collectLinks(
            CopperGolemVisualizationPayload.PosEntry source,
            List<CopperGolemVisualizationPayload.PosEntry> destinations,
            String mode,
            String dimension) {
        SimpleGizmoCollector collector = new SimpleGizmoCollector();
        try (var ignored = Gizmos.withCollector(collector)) {
            CopperGolemVisualizationClient.addContainerLinks(
                    GOLEM_CENTER,
                    source,
                    destinations,
                    mode,
                    dimension
            );
        }
        return collector;
    }

    private static CopperGolemVisualizationPayload.PosEntry pos(
            int x,
            int y,
            int z,
            boolean available) {
        return new CopperGolemVisualizationPayload.PosEntry(OVERWORLD, x, y, z, available);
    }

    private static void assertLink(
            SimpleGizmoCollector collector,
            int index,
            CopperGolemVisualizationPayload.PosEntry target,
            int color) {
        var instance = collector.getGizmos().get(index);
        LineGizmo line = assertInstanceOf(LineGizmo.class, instance.gizmo());
        assertEquals(GOLEM_CENTER, line.start());
        assertEquals(new Vec3(target.x() + 0.5D, target.y() + 0.5D, target.z() + 0.5D), line.end());
        assertEquals(color, line.color());
        assertEquals(2.0F, line.width());
        assertFalse(instance.isAlwaysOnTop());
    }
}
