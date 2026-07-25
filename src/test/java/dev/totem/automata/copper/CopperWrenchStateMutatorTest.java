package dev.totem.automata.copper;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CopperWrenchStateMutatorTest {
    private static CopperGolemBinding A;

    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        Bootstrap.validate();
        A = new CopperGolemBinding(Level.OVERWORLD, new BlockPos(1, 64, 1));
    }

    @Test void addsRemovesAndBumpsRevisionForDestinationBindings() {
        CompoundTag tag = new CompoundTag(); assertTrue(CopperWrenchStateMutator.addBinding(tag, A));
        assertFalse(CopperWrenchStateMutator.addBinding(tag, A)); assertEquals(1, SortingBindingService.getBindings(tag).size());
        assertTrue(CopperWrenchStateMutator.removeBinding(tag, A)); assertTrue(SortingBindingService.getBindings(tag).isEmpty());
        assertEquals(2, tag.getIntOr(CopperGolemData.TAG_REVISION, 0));
    }
    @Test void sourceRemovesMatchingDestinationAndResetsSearch() {
        CompoundTag tag = new CompoundTag(); CopperWrenchStateMutator.addBinding(tag, A); tag.putLong(GatheringRuntimeState.SCAN_INDEX, 9);
        assertTrue(CopperWrenchStateMutator.setSource(tag, A));
        assertEquals(A, SortingBindingService.getSourceContainer(tag).orElseThrow()); assertTrue(SortingBindingService.getBindings(tag).isEmpty());
        assertFalse(tag.contains(GatheringRuntimeState.SCAN_INDEX));
    }
    @Test void removingDestinationPrunesItsLlmConfigWithoutResettingGatheringSearch() {
        CompoundTag tag = new CompoundTag();
        CopperWrenchStateMutator.addBinding(tag, A);
        SortingLlmState.configure(tag, A, true, "ores");
        tag.putLong(GatheringRuntimeState.SCAN_INDEX, 9);

        assertTrue(CopperWrenchStateMutator.removeBinding(tag, A));

        assertTrue(SortingLlmState.read(tag).isEmpty());
        assertTrue(tag.contains(GatheringRuntimeState.SCAN_INDEX));
    }
    @Test void gatheringCornerAndTargetMutationsResetSearch() {
        CompoundTag tag = new CompoundTag(); tag.putLong(GatheringRuntimeState.SCAN_INDEX, 9);
        assertEquals(GatheringConfiguration.CornerUpdate.UPDATED, CopperWrenchStateMutator.setGatheringCorner(tag, Level.OVERWORLD, BlockPos.ZERO, false));
        assertFalse(tag.contains(GatheringRuntimeState.SCAN_INDEX)); assertTrue(CopperWrenchStateMutator.toggleGatheringTarget(tag, "minecraft:stone"));
        assertEquals(java.util.List.of("minecraft:stone"), GatheringConfiguration.manualTargets(tag));
    }
}
