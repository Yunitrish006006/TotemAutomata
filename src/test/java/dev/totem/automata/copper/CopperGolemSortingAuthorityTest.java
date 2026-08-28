package dev.totem.automata.copper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CopperGolemSortingAuthorityTest {
    @Test void derivesAllMixinAuthorityFieldsFromOneImmutableTagSnapshot() {
        CompoundTag tag = new CompoundTag();
        tag.putString(CopperGolemData.TAG_MODE, CopperGolemMode.SORTING.id());
        tag.putBoolean(CopperGolemData.TAG_TRANSPORT_ENABLED, true);
        tag.putBoolean("deadrecall_sorting_blocked", true);
        var dimension = ResourceKey.create(
                Registries.DIMENSION,
                Identifier.fromNamespaceAndPath("totem_automata_test", "sorting_snapshot")
        );
        CopperGolemBinding source = new CopperGolemBinding(dimension, new BlockPos(4, 5, 6));
        SortingBindingService.writeSourceContainer(tag, source);
        CopperGolemData.writeBindings(tag, List.of(
                new CopperGolemBinding(dimension, new BlockPos(7, 8, 9))));

        CopperGolemSortingAuthority.State snapshot = CopperGolemSortingAuthority.snapshot(tag);
        tag.putBoolean(CopperGolemData.TAG_TRANSPORT_ENABLED, false);

        assertTrue(snapshot.sortingMode());
        assertTrue(snapshot.hasBinding());
        assertTrue(snapshot.transportEnabled());
        assertTrue(snapshot.sortingBlocked());
        assertEquals(source, snapshot.source().orElseThrow());
        assertTrue(snapshot.authorityTag()
                        .getBooleanOr(CopperGolemData.TAG_TRANSPORT_ENABLED, false),
                "later tag mutations must not alter the authority snapshot");
    }
}
