package dev.totem.automata.copper;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

/** Regression coverage for recipe-aware Copper Golem furnace destinations. */
public final class FurnaceDestinationGameTest {
    private static final BlockPos CONTAINER_POS = new BlockPos(2, 2, 2);

    @GameTest(maxTicks = 20)
    public void furnaceOnlyAcceptsRecipeInputsAndKeepsFuelAndResultSlotsSeparate(GameTestHelper helper) {
        Container furnace = placeContainer(helper, Blocks.FURNACE);
        if (furnace == null) return;

        require(helper, !SortingDestinationService.canAccept(furnace, new ItemStack(Items.DIAMOND)),
                "Furnace accepted an item without a smelting recipe");
        require(helper, SortingDestinationService.canAccept(furnace, new ItemStack(Items.RAW_IRON)),
                "Furnace rejected an input with a non-empty smelting result");

        ItemStack dualPurposeRemainder = SortingDestinationService.insert(
                furnace, new ItemStack(Items.OAK_LOG));
        require(helper, dualPurposeRemainder.isEmpty() && furnace.getItem(0).is(Items.OAK_LOG)
                        && furnace.getItem(1).isEmpty(),
                "A fuel item with a smelting result was not accepted by the furnace input slot");
        furnace.clearContent();

        ItemStack rawRemainder = SortingDestinationService.insert(furnace, new ItemStack(Items.RAW_IRON, 2));
        require(helper, rawRemainder.isEmpty() && furnace.getItem(0).is(Items.RAW_IRON)
                        && furnace.getItem(0).getCount() == 2,
                "Smeltable input was not inserted into the furnace input slot");
        ItemStack fuelRemainder = SortingDestinationService.insert(furnace, new ItemStack(Items.COAL, 3));
        require(helper, fuelRemainder.isEmpty() && furnace.getItem(1).is(Items.COAL)
                        && furnace.getItem(1).getCount() == 3,
                "Fuel was not inserted exclusively into the furnace fuel slot");
        require(helper, furnace.getItem(2).isEmpty(), "Automata inserted an item into the furnace result slot");
        helper.succeed();
    }

    @GameTest(maxTicks = 20)
    public void smokerAndBlastFurnaceUseTheirOwnRecipeTypes(GameTestHelper helper) {
        Container smoker = placeContainer(helper, Blocks.SMOKER);
        if (smoker == null) return;
        require(helper, !SortingDestinationService.canAccept(smoker, new ItemStack(Items.COBBLESTONE)),
                "Smoker accepted an input that only has a regular smelting recipe");
        require(helper, SortingDestinationService.canAccept(smoker, new ItemStack(Items.BEEF)),
                "Smoker rejected an input with a smoking recipe");

        Container blastFurnace = placeContainer(helper, Blocks.BLAST_FURNACE);
        if (blastFurnace == null) return;
        require(helper, !SortingDestinationService.canAccept(blastFurnace, new ItemStack(Items.BEEF)),
                "Blast furnace accepted an input without a blasting recipe");
        require(helper, SortingDestinationService.canAccept(blastFurnace, new ItemStack(Items.RAW_IRON)),
                "Blast furnace rejected an input with a blasting recipe");
        helper.succeed();
    }

    @GameTest(maxTicks = 20)
    public void capacityChecksIgnoreFuelAndResultSlotsForRecipeInputs(GameTestHelper helper) {
        Container furnace = placeContainer(helper, Blocks.FURNACE);
        if (furnace == null) return;
        furnace.setItem(0, new ItemStack(Items.RAW_IRON, 64));
        furnace.setItem(1, new ItemStack(Items.COAL));
        furnace.setItem(2, new ItemStack(Items.IRON_INGOT));

        require(helper, !SortingDestinationService.hasAvailableSpace(furnace, new ItemStack(Items.RAW_IRON)),
                "Fuel or result slot was counted as additional furnace input capacity");
        ItemStack remainder = SortingDestinationService.insert(furnace, new ItemStack(Items.RAW_IRON));
        require(helper, remainder.is(Items.RAW_IRON) && remainder.getCount() == 1,
                "Full furnace input did not preserve the uninserted item");
        require(helper, furnace.getItem(1).is(Items.COAL) && furnace.getItem(2).is(Items.IRON_INGOT),
                "Rejected input changed the furnace fuel or result slot");
        helper.succeed();
    }

    @GameTest(maxTicks = 20)
    public void gatheringUsesTheSameFurnaceRulesAndOrdinaryContainersStayCompatible(GameTestHelper helper) {
        Container furnace = placeContainer(helper, Blocks.FURNACE);
        if (furnace == null) return;
        require(helper, !GatheringDeposit.canInsertAll(furnace, List.of(new ItemStack(Items.DIAMOND))),
                "Gathering simulation accepted a non-smeltable furnace input");
        require(helper, !GatheringDeposit.insertAll(furnace, List.of(new ItemStack(Items.DIAMOND))),
                "Gathering insertion placed a non-smeltable item into a furnace");
        require(helper, furnace.isEmpty(), "Rejected gathering deposit changed the furnace");

        List<ItemStack> furnaceLoad = List.of(new ItemStack(Items.RAW_IRON, 2), new ItemStack(Items.COAL, 3));
        require(helper, GatheringDeposit.canInsertAll(furnace, furnaceLoad),
                "Gathering simulation rejected valid furnace input and fuel");
        require(helper, GatheringDeposit.insertAll(furnace, furnaceLoad),
                "Gathering insertion rejected valid furnace input and fuel");
        require(helper, furnace.getItem(0).is(Items.RAW_IRON) && furnace.getItem(1).is(Items.COAL)
                        && furnace.getItem(2).isEmpty(),
                "Gathering did not preserve furnace input, fuel, and result slot roles");

        Container barrel = placeContainer(helper, Blocks.BARREL);
        if (barrel == null) return;
        barrel.setItem(0, new ItemStack(Items.DIAMOND));
        require(helper, SortingDestinationService.canAccept(barrel, new ItemStack(Items.DIAMOND)),
                "Recipe-aware furnace rules changed ordinary-container acceptance");
        require(helper, SortingDestinationService.insert(barrel, new ItemStack(Items.DIAMOND)).isEmpty()
                        && barrel.getItem(0).getCount() == 2,
                "Recipe-aware furnace rules changed ordinary-container insertion");
        helper.succeed();
    }

    private static Container placeContainer(GameTestHelper helper, Block block) {
        helper.setBlock(CONTAINER_POS, block);
        if (helper.getLevel().getBlockEntity(helper.absolutePos(CONTAINER_POS)) instanceof Container container) {
            return container;
        }
        helper.fail("Could not create test container for " + block);
        return null;
    }

    private static void require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) throw helper.assertionException(message);
    }
}
