package dev.totem.automata.copper;

import net.minecraft.SharedConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatheringStorageTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        RegistryAccess.Frozen builtInLookup = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        Set<?> builtInRegistryKeys = builtInLookup.listRegistryKeys().collect(Collectors.toSet());
        HolderLookup.Provider lookup = HolderLookup.Provider.create(Stream.concat(
                builtInLookup.listRegistries(),
                VanillaRegistries.createLookup().listRegistries()
                        .filter(registry -> !builtInRegistryKeys.contains(registry.key()))));
        BuiltInRegistries.DATA_COMPONENT_INITIALIZERS
                .build(lookup)
                .forEach(initializer -> initializer.apply());
        Bootstrap.validate();
    }

    @Test
    void mixedTypesShareOneTotalCapacity() {
        List<ItemStack> storage = List.of(
                new ItemStack(Items.COBBLESTONE, 6),
                new ItemStack(Items.COAL, 3),
                new ItemStack(Items.RAW_IRON, 2)
        );
        List<ItemStack> drops = List.of(
                new ItemStack(Items.RAW_COPPER, 4),
                new ItemStack(Items.FLINT, 1)
        );

        assertTrue(GatheringStorage.canStore(storage, drops));
        List<ItemStack> combined = GatheringStorage.addDrops(storage, drops);
        assertEquals(16, GatheringStorage.totalCount(combined));
        assertTrue(GatheringStorage.full(combined));
        assertEquals(5, combined.size());
    }

    @Test
    void mixedDropsAreRejectedOnlyWhenTotalCapacityWouldOverflow() {
        List<ItemStack> storage = List.of(new ItemStack(Items.COBBLESTONE, 10));
        assertTrue(GatheringStorage.canStore(storage, List.of(new ItemStack(Items.COAL, 6))));
        assertFalse(GatheringStorage.canStore(storage, List.of(new ItemStack(Items.COAL, 7))));
    }

    @Test
    void normalizationKeepsDifferentDropKinds() {
        var normalized = GatheringStorage.normalizeDrops(List.of(
                new ItemStack(Items.COBBLESTONE, 2),
                new ItemStack(Items.COAL, 1),
                new ItemStack(Items.COBBLESTONE, 3)
        )).orElseThrow();

        assertEquals(2, normalized.size());
        assertEquals(6, GatheringStorage.totalCount(normalized));
        assertTrue(normalized.stream().anyMatch(stack -> stack.is(Items.COBBLESTONE) && stack.getCount() == 5));
        assertTrue(normalized.stream().anyMatch(stack -> stack.is(Items.COAL) && stack.getCount() == 1));
    }
}
