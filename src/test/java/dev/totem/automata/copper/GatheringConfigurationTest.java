package dev.totem.automata.copper;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GatheringConfigurationTest {
    @Test void persistsACompleteWithinLimitAreaUsingLegacyKeys() {
        CompoundTag tag = new CompoundTag();
        assertEquals(GatheringConfiguration.CornerUpdate.UPDATED, GatheringConfiguration.setCorner(tag, Level.OVERWORLD, new BlockPos(1, 64, 1), false));
        assertEquals(GatheringConfiguration.CornerUpdate.UPDATED, GatheringConfiguration.setCorner(tag, Level.OVERWORLD, new BlockPos(64, 80, 64), true));
        var area = GatheringConfiguration.readArea(tag).orElseThrow();
        assertEquals(Level.OVERWORLD, area.dimension()); assertTrue(area.complete());
        assertTrue(tag.contains(GatheringConfiguration.AREA_DIMENSION));
    }

    @Test void rejectsOversizeSecondCornerWithoutReplacingExistingArea() {
        CompoundTag tag = new CompoundTag();
        GatheringConfiguration.setCorner(tag, Level.OVERWORLD, BlockPos.ZERO, false);
        assertEquals(GatheringConfiguration.CornerUpdate.TOO_LARGE, GatheringConfiguration.setCorner(tag, Level.OVERWORLD, new BlockPos(64, 0, 0), true));
        assertTrue(GatheringConfiguration.readArea(tag).orElseThrow().cornerB().isEmpty());
    }
    @Test void producesRuntimeScanBoundsOnlyForTheCurrentCompleteArea() {
        CompoundTag tag = new CompoundTag(); GatheringConfiguration.setCorner(tag, Level.OVERWORLD, new BlockPos(4, 70, 8), false);
        GatheringConfiguration.setCorner(tag, Level.OVERWORLD, new BlockPos(1, 64, 3), true);
        var bounds = GatheringConfiguration.scanBounds(tag, Level.OVERWORLD).orElseThrow();
        assertEquals(1, bounds.minX()); assertEquals(64, bounds.minY()); assertEquals(3, bounds.minZ()); assertEquals(4, bounds.maxX());
    }

    @Test void togglesTargetsAndKeepsTheMostRecentSixtyFour() {
        CompoundTag tag = new CompoundTag();
        assertTrue(GatheringConfiguration.toggleManualTarget(tag, "minecraft:stone"));
        assertFalse(GatheringConfiguration.toggleManualTarget(tag, "minecraft:stone"));
        for (int i = 0; i < 65; i++) GatheringConfiguration.toggleManualTarget(tag, "minecraft:test_" + i);
        assertEquals(64, GatheringConfiguration.manualTargets(tag).size());
        assertFalse(GatheringConfiguration.manualTargets(tag).contains("minecraft:test_0"));
        assertTrue(GatheringConfiguration.manualTargets(tag).contains("minecraft:test_64"));
    }
}
