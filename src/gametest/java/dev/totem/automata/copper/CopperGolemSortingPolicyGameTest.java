package dev.totem.automata.copper;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.TransportItemsBetweenContainers;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

/** Exercises sorting behavior that must remain available without an LLM classifier. */
public final class CopperGolemSortingPolicyGameTest {
    private static final BlockPos SOURCE_POS = new BlockPos(2, 2, 2);
    private static final BlockPos TARGET_POS = new BlockPos(5, 2, 2);

    @GameTest(maxTicks = 20)
    public void sortsToBoundTargetWhenLlmIsNotConfigured(GameTestHelper helper) {
        TestSetup setup = setup(helper);
        if (setup == null) {
            return;
        }

        SortingLlmState.Config config = SortingLlmState.get(CopperGolemData.readEntityTag(setup.golem()), setup.targetBinding());
        if (config.enabled()) {
            helper.fail("Sorting LLM was unexpectedly enabled in the no-LLM scenario");
            return;
        }
        if (!setup.operations().hasAnySortableItem(setup.golem(), helper.getLevel(), setup.source(), helper.absolutePos(SOURCE_POS))) {
            helper.fail("A bound barrel did not accept an item while no sorting LLM was configured");
            return;
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 20)
    public void manualDenyExcludesItemFromTargetWhenLlmIsDisabled(GameTestHelper helper) {
        TestSetup setup = setup(helper);
        if (setup == null) {
            return;
        }

        CompoundTag tag = CopperGolemData.readEntityTag(setup.golem());
        CopperGolemStateMutation.moveBindingLlmCache(tag, setup.targetBinding(), "minecraft:diamond", false, false);
        CopperGolemData.writeEntityTag(setup.golem(), tag);

        SortingLlmState.Config config = SortingLlmState.get(CopperGolemData.readEntityTag(setup.golem()), setup.targetBinding());
        if (config.enabled() || !config.deniedItemIds().contains("minecraft:diamond")) {
            helper.fail("Manual item deny was coupled to the sorting LLM configuration");
            return;
        }
        if (setup.operations().hasAnySortableItem(setup.golem(), helper.getLevel(), setup.source(), helper.absolutePos(SOURCE_POS))) {
            helper.fail("A manually denied diamond was still considered sortable into the bound barrel");
            return;
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 20)
    public void picksFirstSortableSlotInsteadOfFirstOccupiedSlot(GameTestHelper helper) {
        TestSetup setup = setup(helper);
        if (setup == null) {
            return;
        }

        setup.source().setItem(0, new ItemStack(Items.DIRT));
        setup.source().setItem(1, new ItemStack(Items.DIAMOND));
        giveOneCoal(setup.golem());

        ItemStack picked = SortingModeController.pickUp(
                setup.golem(), helper.getLevel(), setup.source(), helper.absolutePos(SOURCE_POS), setup.operations());
        if (!picked.is(Items.DIAMOND)) {
            helper.fail("Sorting picked the first occupied slot instead of the first slot with a valid destination");
            return;
        }
        if (!setup.source().getItem(0).is(Items.DIRT) || !setup.source().getItem(1).isEmpty()) {
            helper.fail("Sorting changed the wrong source slot");
            return;
        }
        if (setup.operations().rememberedSource(setup.golem()).map(SortingOperations.Source::slot).orElse(-1) != 1) {
            helper.fail("Sorting did not remember the actual source slot");
            return;
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 20)
    public void doesNotPickOrConsumeFuelWhenNoDestinationAcceptsItem(GameTestHelper helper) {
        TestSetup setup = setup(helper);
        if (setup == null) {
            return;
        }

        CompoundTag tag = CopperGolemData.readEntityTag(setup.golem());
        CopperGolemStateMutation.moveBindingLlmCache(tag, setup.targetBinding(), "minecraft:diamond", false, false);
        CopperGolemFuelService.writeFuelStack(tag, new ItemStack(Items.COAL));
        CopperGolemData.writeEntityTag(setup.golem(), tag);

        ItemStack picked = SortingModeController.pickUp(
                setup.golem(), helper.getLevel(), setup.source(), helper.absolutePos(SOURCE_POS), setup.operations());
        CompoundTag result = CopperGolemData.readEntityTag(setup.golem());
        if (!picked.isEmpty() || !setup.source().getItem(0).is(Items.DIAMOND)) {
            helper.fail("Sorting removed an item even though no destination accepted it");
            return;
        }
        if (!CopperGolemFuelService.readFuelStack(result).is(Items.COAL)
                || result.getIntOr(CopperGolemData.TAG_FUEL_TICKS, 0) != 0) {
            helper.fail("Sorting consumed fuel even though it did not pick up an item");
            return;
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 20)
    public void waitsForLlmDecisionWithoutPickingFuelOrBlocking(GameTestHelper helper) {
        TestSetup setup = setup(helper);
        if (setup == null) {
            return;
        }

        CompoundTag tag = CopperGolemData.readEntityTag(setup.golem());
        GolemLlmState.write(tag, new GolemLlmState.Config("http://127.0.0.1:1", "", "test-model"));
        SortingLlmState.configure(tag, setup.targetBinding(), true, "Only valuable gems");
        CopperGolemFuelService.writeFuelStack(tag, new ItemStack(Items.COAL));
        CopperGolemData.writeEntityTag(setup.golem(), tag);
        SortingOperations operations = new ClassifyingSortingOperations(new DefaultItemMetadata(), (server, golemId,
                binding, prompt, itemId, itemTags, decision) -> { });

        ItemStack picked = SortingModeController.pickUp(
                setup.golem(), helper.getLevel(), setup.source(), helper.absolutePos(SOURCE_POS), operations);
        CompoundTag result = CopperGolemData.readEntityTag(setup.golem());
        SortingLlmClassifier.clearPendingRequests();
        if (!picked.isEmpty() || !setup.source().getItem(0).is(Items.DIAMOND)) {
            helper.fail("Sorting picked an item before its LLM decision was available");
            return;
        }
        if (!CopperGolemFuelService.readFuelStack(result).is(Items.COAL)
                || result.getIntOr(CopperGolemData.TAG_FUEL_TICKS, 0) != 0) {
            helper.fail("Sorting consumed fuel while waiting for its LLM decision");
            return;
        }
        if (result.getBooleanOr("deadrecall_sorting_blocked", false)) {
            helper.fail("Sorting entered the blocked state while waiting for its LLM decision");
            return;
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 20)
    public void liveMixinSkipsUnmatchedFirstSlotWithoutLlm(GameTestHelper helper) {
        TestSetup setup = setup(helper);
        if (setup == null) {
            return;
        }

        setup.source().setItem(0, new ItemStack(Items.DIRT));
        setup.source().setItem(1, new ItemStack(Items.DIAMOND));
        startSorting(setup.golem(), helper, setup.targetBinding());
        if (!invokeLivePickup(helper, setup.golem(), setup.source())) {
            return;
        }
        if (!setup.golem().getMainHandItem().is(Items.DIAMOND)
                || !setup.source().getItem(0).is(Items.DIRT)
                || !setup.source().getItem(1).isEmpty()) {
            helper.fail("Live sorting mixin did not pick the matched slot while preserving the unmatched first slot");
            return;
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 20)
    public void liveMixinLeavesOnlyUnmatchedItemInSourceWithoutLlm(GameTestHelper helper) {
        TestSetup setup = setup(helper);
        if (setup == null) {
            return;
        }

        setup.source().setItem(0, new ItemStack(Items.DIRT));
        startSorting(setup.golem(), helper, setup.targetBinding());
        if (!invokeLivePickup(helper, setup.golem(), setup.source())) {
            return;
        }
        CompoundTag result = CopperGolemData.readEntityTag(setup.golem());
        if (!setup.source().getItem(0).is(Items.DIRT) || !setup.golem().getMainHandItem().isEmpty()) {
            helper.fail("Live sorting mixin picked an item with no matching destination while no LLM was configured");
            return;
        }
        if (!CopperGolemFuelService.readFuelStack(result).is(Items.COAL)
                || result.getIntOr(CopperGolemData.TAG_FUEL_TICKS, 0) != 0) {
            helper.fail("Live sorting mixin consumed fuel for an unmatched item");
            return;
        }
        helper.succeed();
    }

    private static TestSetup setup(GameTestHelper helper) {
        CopperGolem golem = CopperGolemDirectInteractionGameTest.spawnCopperGolem(helper);
        if (golem == null) {
            helper.fail("Could not spawn a Copper Golem for the sorting policy test");
            return null;
        }
        Block copperChest = BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("minecraft", "copper_chest"));
        if (copperChest == null || copperChest == Blocks.AIR) {
            helper.fail("Missing minecraft:copper_chest block");
            return null;
        }
        helper.setBlock(SOURCE_POS, copperChest);
        helper.setBlock(TARGET_POS, Blocks.BARREL);
        if (!(helper.getLevel().getBlockEntity(helper.absolutePos(SOURCE_POS)) instanceof Container source)
                || !(helper.getLevel().getBlockEntity(helper.absolutePos(TARGET_POS)) instanceof Container target)) {
            helper.fail("Could not create the source copper chest and target barrel containers");
            return null;
        }
        source.setItem(0, new ItemStack(Items.DIAMOND));
        // Ordinary sorting uses the contents already in a destination as its
        // category hint. Seed the barrel with the same item before exercising
        // the manual policy layered on top of that default behavior.
        target.setItem(0, new ItemStack(Items.DIAMOND));
        CopperGolemBinding targetBinding = new CopperGolemBinding(helper.getLevel().dimension(), helper.absolutePos(TARGET_POS));
        CompoundTag tag = CopperGolemData.readEntityTag(golem);
        CopperGolemData.writeBindings(tag, List.of(targetBinding));
        CopperGolemData.writeEntityTag(golem, tag);
        return new TestSetup(golem, source, targetBinding, new PersistedSortingOperations(new DefaultItemMetadata()));
    }

    private static void startSorting(CopperGolem golem, GameTestHelper helper, CopperGolemBinding targetBinding) {
        CompoundTag tag = CopperGolemData.readEntityTag(golem);
        tag.putString(CopperGolemData.TAG_MODE, CopperGolemMode.SORTING.id());
        tag.putBoolean(CopperGolemData.TAG_TRANSPORT_ENABLED, true);
        SortingBindingService.writeSourceContainer(tag,
                new CopperGolemBinding(helper.getLevel().dimension(), helper.absolutePos(SOURCE_POS)));
        CopperGolemData.writeBindings(tag, List.of(targetBinding));
        CopperGolemFuelService.writeFuelStack(tag, new ItemStack(Items.COAL));
        CopperGolemData.writeEntityTag(golem, tag);
    }

    private static boolean invokeLivePickup(GameTestHelper helper, CopperGolem golem, Container source) {
        try {
            TransportItemsBetweenContainers behavior = new TransportItemsBetweenContainers(
                    1.0F, state -> true, state -> true, 16, 8, Map.of(), mob -> { }, target -> false);
            Field targetField = TransportItemsBetweenContainers.class.getDeclaredField("target");
            targetField.setAccessible(true);
            targetField.set(behavior, TransportItemsBetweenContainers.TransportItemTarget.tryCreatePossibleTarget(
                    helper.absolutePos(SOURCE_POS), helper.getLevel()));
            Method pickup = TransportItemsBetweenContainers.class.getDeclaredMethod(
                    "pickUpItems", PathfinderMob.class, Container.class);
            pickup.setAccessible(true);
            pickup.invoke(behavior, golem, source);
            return true;
        } catch (ReflectiveOperationException exception) {
            helper.fail("Could not invoke the transformed live pickup method: " + exception);
            return false;
        }
    }

    private static void giveOneCoal(CopperGolem golem) {
        CompoundTag tag = CopperGolemData.readEntityTag(golem);
        CopperGolemFuelService.writeFuelStack(tag, new ItemStack(Items.COAL));
        CopperGolemData.writeEntityTag(golem, tag);
    }

    private record TestSetup(CopperGolem golem, Container source, CopperGolemBinding targetBinding,
                             PersistedSortingOperations operations) { }
}
