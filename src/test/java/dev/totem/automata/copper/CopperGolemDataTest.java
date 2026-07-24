package dev.totem.automata.copper;

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

class CopperGolemDataTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        Bootstrap.validate();
    }

    @Test
    void migrateAddsDefaultsAndConvertsLegacyBinding() {
        CompoundTag tag = new CompoundTag();
        tag.putString(CopperGolemData.TAG_BOUND_CONTAINER_DIM, Level.OVERWORLD.identifier().toString());
        tag.putInt(CopperGolemData.TAG_BOUND_CONTAINER_X, 12);
        tag.putInt(CopperGolemData.TAG_BOUND_CONTAINER_Y, 64);
        tag.putInt(CopperGolemData.TAG_BOUND_CONTAINER_Z, -8);

        assertTrue(CopperGolemData.migrate(tag));
        assertEquals(CopperGolemData.DATA_VERSION, tag.getIntOr(CopperGolemData.TAG_DATA_VERSION, -1));
        assertEquals(CopperGolemMode.SORTING.id(), tag.getStringOr(CopperGolemData.TAG_MODE, ""));
        assertEquals(0, tag.getIntOr(CopperGolemData.TAG_REVISION, -1));
        assertFalse(tag.contains(CopperGolemData.TAG_BOUND_CONTAINER_DIM));
        assertEquals(List.of(new CopperGolemBinding(Level.OVERWORLD, new BlockPos(12, 64, -8))),
                CopperGolemData.readBindings(tag));
        assertFalse(CopperGolemData.migrate(tag));
    }

    @Test
    void bindingListRoundTripsWithoutDuplicateEntries() {
        CompoundTag tag = new CompoundTag();
        List<CopperGolemBinding> bindings = List.of(
                new CopperGolemBinding(Level.OVERWORLD, new BlockPos(1, 2, 3)),
                new CopperGolemBinding(Level.NETHER, new BlockPos(-4, 70, 9)));
        CopperGolemData.writeBindings(tag, bindings);
        assertEquals(bindings, CopperGolemData.readBindings(tag));
    }

    @Test
    void sortingLlmStateRetainsLegacyBindingAndClearsCacheWhenPromptChanges() {
        CopperGolemBinding binding = new CopperGolemBinding(Level.OVERWORLD, new BlockPos(3, 65, -7));
        CompoundTag tag = new CompoundTag();
        SortingLlmState.configure(tag, binding, true, "ores");
        SortingLlmState.recordDecision(tag, binding, "minecraft:iron_ore", List.of("minecraft:ores"), true, List.of("minecraft:ores"));
        assertEquals(true, SortingLlmState.get(tag, binding).enabled());
        assertEquals(List.of("minecraft:iron_ore"), SortingLlmState.get(tag, binding).allowedItemIds());
        SortingLlmState.configure(tag, binding, true, "gems");
        assertTrue(SortingLlmState.get(tag, binding).allowedItemIds().isEmpty());
        assertTrue(SortingLlmState.get(tag, binding).allowedTags().isEmpty());
    }

    @Test
    void golemLlmStateUsesThePreservedConnectionKeys() {
        CompoundTag tag = new CompoundTag();
        GolemLlmState.write(tag, new GolemLlmState.Config("https://example.invalid/v1", "key", "model"));
        assertTrue(GolemLlmState.read(tag).configured());
        assertEquals("model", GolemLlmState.read(tag).model());
    }
}
