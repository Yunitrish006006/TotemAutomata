package dev.totem.automata.copper;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/** Runtime regression coverage for the shared-capacity mixed gathering backpack. */
public final class GatheringMixedStorageGameTest {
    private static final String LEGACY_STORAGE = "deadrecall_gathering_storage_stack";

    @GameTest(maxTicks = 20)
    public void mixedKindsRoundTripAndDepositTogether(GameTestHelper helper) {
        var level = helper.getLevel();
        CompoundTag tag = new CompoundTag();
        List<ItemStack> carried = List.of(
                new ItemStack(Items.COBBLESTONE, 6),
                new ItemStack(Items.COAL, 3),
                new ItemStack(Items.RAW_IRON, 2),
                new ItemStack(Items.RAW_COPPER, 4),
                new ItemStack(Items.FLINT, 1)
        );

        GatheringStorage.write(tag, carried, level.registryAccess());
        List<ItemStack> restored = GatheringStorage.read(tag, level.registryAccess());
        require(helper, GatheringStorage.totalCount(restored) == 16, "Mixed storage did not preserve the shared total count");
        require(helper, restored.size() == 5, "Mixed storage did not preserve all item kinds");
        require(helper, GatheringStorage.full(restored), "Sixteen carried items were not treated as full");

        SimpleContainer home = new SimpleContainer(9);
        require(helper, GatheringDeposit.canInsertAll(home, restored), "Home simulation rejected a valid mixed deposit");
        require(helper, GatheringDeposit.insertAll(home, restored), "Mixed carried items were not deposited together");
        int deposited = 0;
        for (int slot = 0; slot < home.getContainerSize(); slot++) deposited += home.getItem(slot).getCount();
        require(helper, deposited == 16, "Mixed deposit lost or duplicated carried items");
        helper.succeed();
    }

    @GameTest(maxTicks = 20)
    public void legacySingleStackLoadsIntoNewStorageWithoutLoss(GameTestHelper helper) {
        var level = helper.getLevel();
        CompoundTag legacy = new CompoundTag();
        CopperGolemData.writeItemStack(legacy, LEGACY_STORAGE, new ItemStack(Items.COBBLESTONE, 7), level.registryAccess());

        List<ItemStack> restored = GatheringStorage.read(legacy, level.registryAccess());
        require(helper, restored.size() == 1, "Legacy carried stack did not migrate as one storage entry");
        require(helper, restored.getFirst().is(Items.COBBLESTONE) && restored.getFirst().getCount() == 7,
                "Legacy carried stack changed item or count during migration");

        List<ItemStack> expanded = GatheringStorage.addDrops(restored, List.of(new ItemStack(Items.COAL, 2)));
        GatheringStorage.write(legacy, expanded, level.registryAccess());
        List<ItemStack> roundTrip = GatheringStorage.read(legacy, level.registryAccess());
        require(helper, roundTrip.size() == 2 && GatheringStorage.totalCount(roundTrip) == 9,
                "Legacy migration could not accept a second item kind");
        helper.succeed();
    }

    private static void require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) throw helper.assertionException(message);
    }
}
