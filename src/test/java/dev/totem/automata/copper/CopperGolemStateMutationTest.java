package dev.totem.automata.copper;

import dev.totem.automata.network.CopperGolemGatheringTargetPayload;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CopperGolemStateMutationTest {
    private static CopperGolemBinding binding;

    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        Bootstrap.validate();
        binding = new CopperGolemBinding(Level.OVERWORLD, new BlockPos(2, 70, -3));
    }

    @Test
    void operationAndModeMutationsClearLegacyRuntimeStateAndAdvanceRevision() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("deadrecall_sorting_blocked", true);
        tag.putLong(GatheringRuntimeState.SCAN_INDEX, 8);
        tag.put("deadrecall_tried_destinations", new net.minecraft.nbt.ListTag());

        CopperGolemStateMutation.setTransportEnabled(tag, true);
        assertTrue(tag.getBooleanOr(CopperGolemData.TAG_TRANSPORT_ENABLED, false));
        assertFalse(tag.contains("deadrecall_sorting_blocked"));
        assertEquals(1, tag.getIntOr(CopperGolemData.TAG_REVISION, 0));

        CopperGolemStateMutation.setMode(tag, CopperGolemMode.GATHERING);
        assertEquals(CopperGolemMode.GATHERING.id(), tag.getStringOr(CopperGolemData.TAG_MODE, ""));
        assertFalse(tag.contains(GatheringRuntimeState.SCAN_INDEX));
        assertFalse(tag.contains("deadrecall_tried_destinations"));
        assertEquals(2, tag.getIntOr(CopperGolemData.TAG_REVISION, 0));
    }

    @Test
    void bindingAndGatheringLlmMutationsPreserveCacheRules() {
        CompoundTag tag = new CompoundTag();
        CopperGolemStateMutation.configureBindingLlm(tag, binding, true, "ores");
        CopperGolemStateMutation.moveBindingLlmCache(tag, binding, "minecraft:iron_ore", false, true);
        assertEquals(List.of("minecraft:iron_ore"), SortingLlmState.get(tag, binding).allowedItemIds());

        CopperGolemStateMutation.configureGatheringLlm(tag, true, "mine stone");
        int promptRevision = GatheringLlmState.read(tag).promptRevision();
        assertTrue(GatheringLlmState.recordDecision(tag, "minecraft:stone", List.of(), true, List.of(), promptRevision));
        CopperGolemStateMutation.configureGatheringLlm(tag, true, "mine ores");
        assertTrue(GatheringLlmState.read(tag).allowedBlockIds().isEmpty());
        assertEquals(4, tag.getIntOr(CopperGolemData.TAG_REVISION, 0));
    }

    @Test
    void gatheringTargetRemovalDoesNotAdvanceRevisionWhenNothingChanged() {
        CompoundTag tag = new CompoundTag();
        GatheringConfiguration.toggleManualTarget(tag, "minecraft:stone");
        int before = tag.getIntOr(CopperGolemData.TAG_REVISION, 0);

        assertTrue(CopperGolemStateMutation.removeGatheringTarget(
                tag,
                "minecraft:stone",
                false,
                CopperGolemGatheringTargetPayload.TargetSet.MANUAL
        ));
        assertEquals(before + 1, tag.getIntOr(CopperGolemData.TAG_REVISION, 0));
        assertFalse(CopperGolemStateMutation.removeGatheringTarget(
                tag,
                "minecraft:stone",
                false,
                CopperGolemGatheringTargetPayload.TargetSet.MANUAL
        ));
        assertEquals(before + 1, tag.getIntOr(CopperGolemData.TAG_REVISION, 0));
    }
}
