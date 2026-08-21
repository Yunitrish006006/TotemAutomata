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
import java.util.Optional;

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
        giveOneCoal(setup.golem(), helper.getLevel());

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
        CopperGolemFuelService.writeFuelStack(tag, new ItemStack(Items.COAL), helper.getLevel());
        CopperGolemData.writeEntityTag(setup.golem(), tag);

        ItemStack picked = SortingModeController.pickUp(
                setup.golem(), helper.getLevel(), setup.source(), helper.absolutePos(SOURCE_POS), setup.operations());
        CompoundTag result = CopperGolemData.readEntityTag(setup.golem());
        if (!picked.isEmpty() || !setup.source().getItem(0).is(Items.DIAMOND)) {
            helper.fail("Sorting removed an item even though no destination accepted it");
            return;
        }
        if (!CopperGolemFuelService.readFuelStack(result, helper.getLevel()).is(Items.COAL)
                || result.getIntOr(CopperGolemData.TAG_FUEL_TICKS, 0) != 0) {
            helper.fail("Sorting consumed fuel even though it did not pick up an item");
            return;
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 20)
    public void deniedRouteIsCheckedBeforeDestinationInspectionOrFuelDebit(GameTestHelper helper) {
        TestSetup setup = setup(helper);
        if (setup == null) return;
        RouteGateOperations operations = new RouteGateOperations(false);
        giveOneCoal(setup.golem(), helper.getLevel());

        ItemStack picked = SortingModeController.pickUp(
                setup.golem(), helper.getLevel(), setup.source(),
                helper.absolutePos(SOURCE_POS), operations);
        CompoundTag result = CopperGolemData.readEntityTag(setup.golem());
        if (!picked.isEmpty() || !setup.source().getItem(0).is(Items.DIAMOND)) {
            helper.fail("A denied Locksmith route removed its source item");
            return;
        }
        if (operations.destinationInspections != 0) {
            helper.fail("A denied Locksmith route inspected destination contents");
            return;
        }
        if (!CopperGolemFuelService.readFuelStack(result, helper.getLevel()).is(Items.COAL)
                || result.getIntOr(CopperGolemData.TAG_FUEL_TICKS, 0) != 0) {
            helper.fail("A denied Locksmith route consumed fuel");
            return;
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 20)
    public void permissionChangeAfterPickupLeavesDestinationUntouchedAndReturnsItem(GameTestHelper helper) {
        TestSetup setup = setup(helper);
        if (setup == null) return;
        RouteGateOperations operations = new RouteGateOperations(true);
        giveOneCoal(setup.golem(), helper.getLevel());

        ItemStack picked = SortingModeController.pickUp(
                setup.golem(), helper.getLevel(), setup.source(),
                helper.absolutePos(SOURCE_POS), operations);
        if (!picked.is(Items.DIAMOND)) {
            helper.fail("Authorised route did not pick its source item");
            return;
        }
        setup.golem().setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, picked);
        operations.routeAllowed = false;
        Optional<ItemStack> remaining = SortingModeController.deposit(
                setup.golem(), helper.getLevel(), helper.absolutePos(TARGET_POS),
                setup.target(), operations);
        if (remaining.isEmpty() || !remaining.get().is(Items.DIAMOND)
                || setup.target().getItem(0).getCount() != 1) {
            helper.fail("A route denied after pickup still changed the destination");
            return;
        }
        setup.golem().setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, remaining.get());
        if (!SortingModeController.returnCarried(
                        setup.golem(), helper.getLevel(), operations)
                || !setup.source().getItem(0).is(Items.DIAMOND)
                || !setup.golem().getMainHandItem().isEmpty()) {
            helper.fail("A mid-flight permission change did not safely return the carried item");
            return;
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 20)
    public void sameLockReverseRouteCanReturnRemainderWhenBoundaryInsertIsDenied(GameTestHelper helper) {
        TestSetup setup = setup(helper);
        if (setup == null) return;
        InternalRouteOperations operations = new InternalRouteOperations();
        giveOneCoal(setup.golem(), helper.getLevel());

        ItemStack picked = SortingModeController.pickUp(
                setup.golem(), helper.getLevel(), setup.source(),
                helper.absolutePos(SOURCE_POS), operations);
        setup.golem().setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, picked);
        if (SortingModeController.nextDestination(
                setup.golem(), helper.getLevel(), picked, operations).isEmpty()) {
            helper.fail("Authorised internal route did not resolve its destination");
            return;
        }
        if (!SortingModeController.returnCarried(
                        setup.golem(), helper.getLevel(), operations)
                || !setup.source().getItem(0).is(Items.DIAMOND)
                || !setup.golem().getMainHandItem().isEmpty()) {
            helper.fail("Same-lock reverse route could not return its genuine remainder");
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
        CopperGolemFuelService.writeFuelStack(tag, new ItemStack(Items.COAL), helper.getLevel());
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
        if (!CopperGolemFuelService.readFuelStack(result, helper.getLevel()).is(Items.COAL)
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
        if (!CopperGolemFuelService.readFuelStack(result, helper.getLevel()).is(Items.COAL)
                || result.getIntOr(CopperGolemData.TAG_FUEL_TICKS, 0) != 0) {
            helper.fail("Live sorting mixin consumed fuel for an unmatched item");
            return;
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 20)
    public void liveMixinCommitsFullDepositOnceAcrossLaterTargetResolution(GameTestHelper helper) {
        TestSetup setup = setup(helper);
        if (setup == null) return;

        setup.source().setItem(0, new ItemStack(Items.DIAMOND, 16));
        setup.target().setItem(0, new ItemStack(Items.DIAMOND));
        startSorting(setup.golem(), helper, setup.targetBinding());
        LiveBehavior live = createLiveBehavior(helper);
        if (live == null
                || !invokeLivePickup(helper, live, setup.golem(), setup.source())
                || !invokeLiveDeposit(helper, live, setup.golem(), setup.target())) return;

        if (!setup.source().getItem(0).isEmpty()
                || !setup.golem().getMainHandItem().isEmpty()
                || setup.target().getItem(0).getCount() != 17
                || setup.operations().rememberedSource(setup.golem()).isPresent()) {
            helper.fail("Full live sorting deposit did not commit once and clear its in-flight source");
            return;
        }

        if (!invokeLaterTargetResolution(helper, live, setup.golem())) return;
        if (!setup.source().getItem(0).isEmpty()
                || setup.target().getItem(0).getCount() != 17
                || !setup.golem().getMainHandItem().isEmpty()) {
            helper.fail("A later target-resolution tick replayed or returned a completed deposit");
            return;
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 20)
    public void liveMixinReturnsOnlyPartialDepositRemainder(GameTestHelper helper) {
        TestSetup setup = setup(helper);
        if (setup == null) return;

        setup.source().setItem(0, new ItemStack(Items.DIAMOND, 16));
        setup.target().setItem(0, new ItemStack(Items.DIAMOND, 63));
        fillOtherTargetSlots(setup.target());
        startSorting(setup.golem(), helper, setup.targetBinding());
        LiveBehavior live = createLiveBehavior(helper);
        if (live == null
                || !invokeLivePickup(helper, live, setup.golem(), setup.source())
                || !invokeLiveDeposit(helper, live, setup.golem(), setup.target())) return;

        if (setup.target().getItem(0).getCount() != 64
                || setup.golem().getMainHandItem().getCount() != 15) {
            helper.fail("Partial live sorting deposit did not keep exactly the uninserted remainder");
            return;
        }
        if (!invokeLaterTargetResolution(helper, live, setup.golem())) return;
        if (setup.target().getItem(0).getCount() != 64
                || setup.source().getItem(0).getCount() != 15
                || !setup.golem().getMainHandItem().isEmpty()) {
            helper.fail("Partial live sorting returned more than the genuine remainder");
            return;
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 20)
    public void liveMixinReturnsRejectedDepositWithoutChangingDestination(GameTestHelper helper) {
        TestSetup setup = setup(helper);
        if (setup == null) return;

        startSorting(setup.golem(), helper, setup.targetBinding());
        LiveBehavior live = createLiveBehavior(helper);
        if (live == null || !invokeLivePickup(helper, live, setup.golem(), setup.source())) return;
        CompoundTag tag = CopperGolemData.readEntityTag(setup.golem());
        CopperGolemStateMutation.moveBindingLlmCache(
                tag, setup.targetBinding(), "minecraft:diamond", false, false);
        CopperGolemData.writeEntityTag(setup.golem(), tag);

        if (!invokeLiveDeposit(helper, live, setup.golem(), setup.target())) return;
        if (!setup.golem().getMainHandItem().is(Items.DIAMOND)
                || setup.target().getItem(0).getCount() != 1) {
            helper.fail("A destination rejected after pickup was still mutated");
            return;
        }
        if (!invokeLaterTargetResolution(helper, live, setup.golem())) return;
        if (!setup.source().getItem(0).is(Items.DIAMOND)
                || setup.source().getItem(0).getCount() != 1
                || setup.target().getItem(0).getCount() != 1
                || !setup.golem().getMainHandItem().isEmpty()) {
            helper.fail("A rejected live deposit did not return exactly its carried item");
            return;
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 20)
    public void liveMixinReturnsItemWhenCapacityChangesAfterPickup(GameTestHelper helper) {
        TestSetup setup = setup(helper);
        if (setup == null) return;

        setup.source().setItem(0, new ItemStack(Items.DIAMOND, 16));
        startSorting(setup.golem(), helper, setup.targetBinding());
        LiveBehavior live = createLiveBehavior(helper);
        if (live == null || !invokeLivePickup(helper, live, setup.golem(), setup.source())) return;
        setup.target().setItem(0, new ItemStack(Items.DIAMOND, 64));
        fillOtherTargetSlots(setup.target());
        CompoundTag fuelAfterPickup = CopperGolemData.readEntityTag(setup.golem());
        int remainingFuelTicks = fuelAfterPickup.getIntOr(CopperGolemData.TAG_FUEL_TICKS, 0);

        if (!invokeLiveDeposit(helper, live, setup.golem(), setup.target())
                || !invokeLaterTargetResolution(helper, live, setup.golem())) return;
        CompoundTag result = CopperGolemData.readEntityTag(setup.golem());
        if (setup.source().getItem(0).getCount() != 16
                || setup.target().getItem(0).getCount() != 64
                || !setup.golem().getMainHandItem().isEmpty()) {
            helper.fail("A mid-flight capacity change duplicated, deleted, or deposited the carried stack");
            return;
        }
        if (result.getIntOr(CopperGolemData.TAG_FUEL_TICKS, 0) != remainingFuelTicks) {
            helper.fail("Returning a rejected in-flight stack consumed fuel a second time");
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
        return new TestSetup(golem, source, target, targetBinding,
                new PersistedSortingOperations(new DefaultItemMetadata()));
    }

    private static void startSorting(CopperGolem golem, GameTestHelper helper, CopperGolemBinding targetBinding) {
        CompoundTag tag = CopperGolemData.readEntityTag(golem);
        tag.putString(CopperGolemData.TAG_MODE, CopperGolemMode.SORTING.id());
        tag.putBoolean(CopperGolemData.TAG_TRANSPORT_ENABLED, true);
        SortingBindingService.writeSourceContainer(tag,
                new CopperGolemBinding(helper.getLevel().dimension(), helper.absolutePos(SOURCE_POS)));
        CopperGolemData.writeBindings(tag, List.of(targetBinding));
        CopperGolemFuelService.writeFuelStack(tag, new ItemStack(Items.COAL), helper.getLevel());
        CopperGolemData.writeEntityTag(golem, tag);
    }

    private static boolean invokeLivePickup(GameTestHelper helper, CopperGolem golem, Container source) {
        LiveBehavior live = createLiveBehavior(helper);
        return live != null && invokeLivePickup(helper, live, golem, source);
    }

    private static LiveBehavior createLiveBehavior(GameTestHelper helper) {
        try {
            TransportItemsBetweenContainers behavior = new TransportItemsBetweenContainers(
                    1.0F, state -> true, state -> true, 16, 8, Map.of(), mob -> { }, target -> false);
            Field targetField = TransportItemsBetweenContainers.class.getDeclaredField("target");
            targetField.setAccessible(true);
            return new LiveBehavior(behavior, targetField);
        } catch (ReflectiveOperationException exception) {
            helper.fail("Could not create the transformed live sorting behavior: " + exception);
            return null;
        }
    }

    private static boolean invokeLivePickup(
            GameTestHelper helper,
            LiveBehavior live,
            CopperGolem golem,
            Container source
    ) {
        try {
            live.targetField().set(live.behavior(),
                    TransportItemsBetweenContainers.TransportItemTarget.tryCreatePossibleTarget(
                            helper.absolutePos(SOURCE_POS), helper.getLevel()));
            Method pickup = TransportItemsBetweenContainers.class.getDeclaredMethod(
                    "pickUpItems", PathfinderMob.class, Container.class);
            pickup.setAccessible(true);
            pickup.invoke(live.behavior(), golem, source);
            return true;
        } catch (ReflectiveOperationException exception) {
            helper.fail("Could not invoke the transformed live pickup method: " + exception);
            return false;
        }
    }

    private static boolean invokeLiveDeposit(
            GameTestHelper helper,
            LiveBehavior live,
            CopperGolem golem,
            Container destination
    ) {
        try {
            live.targetField().set(live.behavior(),
                    TransportItemsBetweenContainers.TransportItemTarget.tryCreatePossibleTarget(
                            helper.absolutePos(TARGET_POS), helper.getLevel()));
            Method putDown = TransportItemsBetweenContainers.class.getDeclaredMethod(
                    "putDownItem", PathfinderMob.class, Container.class);
            putDown.setAccessible(true);
            putDown.invoke(live.behavior(), golem, destination);
            return true;
        } catch (ReflectiveOperationException exception) {
            helper.fail("Could not invoke the transformed live deposit method: " + exception);
            return false;
        }
    }

    private static boolean invokeLaterTargetResolution(
            GameTestHelper helper,
            LiveBehavior live,
            CopperGolem golem
    ) {
        try {
            Method getTarget = TransportItemsBetweenContainers.class.getDeclaredMethod(
                    "getTransportTarget", net.minecraft.server.level.ServerLevel.class, PathfinderMob.class);
            getTarget.setAccessible(true);
            Object resolved = getTarget.invoke(live.behavior(), helper.getLevel(), golem);
            if (!(resolved instanceof Optional<?>)) {
                helper.fail("Live target resolution returned an unexpected result");
                return false;
            }
            return true;
        } catch (ReflectiveOperationException exception) {
            helper.fail("Could not invoke the transformed later target-resolution method: " + exception);
            return false;
        }
    }

    private static void fillOtherTargetSlots(Container target) {
        for (int slot = 1; slot < target.getContainerSize(); slot++) {
            target.setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
        }
    }

    private static void giveOneCoal(CopperGolem golem, net.minecraft.server.level.ServerLevel level) {
        CompoundTag tag = CopperGolemData.readEntityTag(golem);
        CopperGolemFuelService.writeFuelStack(tag, new ItemStack(Items.COAL), level);
        CopperGolemData.writeEntityTag(golem, tag);
    }

    private record TestSetup(CopperGolem golem, Container source, Container target,
                             CopperGolemBinding targetBinding,
                             PersistedSortingOperations operations) { }

    private record LiveBehavior(TransportItemsBetweenContainers behavior, Field targetField) { }

    private static final class RouteGateOperations extends PersistedSortingOperations {
        private boolean routeAllowed;
        private int destinationInspections;

        private RouteGateOperations(boolean routeAllowed) {
            super(new DefaultItemMetadata());
            this.routeAllowed = routeAllowed;
        }

        @Override
        public boolean mayTransfer(
                CopperGolem golem,
                net.minecraft.server.level.ServerLevel level,
                BlockPos source,
                BlockPos destination
        ) {
            return routeAllowed;
        }

        @Override
        public boolean canAccept(
                CopperGolem golem,
                net.minecraft.server.level.ServerLevel level,
                CopperGolemBinding binding,
                Container container,
                ItemStack carried
        ) {
            destinationInspections++;
            return super.canAccept(golem, level, binding, container, carried);
        }
    }

    private static final class InternalRouteOperations extends PersistedSortingOperations {
        private InternalRouteOperations() {
            super(new DefaultItemMetadata());
        }

        @Override
        public boolean mayTransfer(
                CopperGolem golem,
                net.minecraft.server.level.ServerLevel level,
                BlockPos source,
                BlockPos destination
        ) {
            return true;
        }

        @Override
        public boolean mayInsert(CopperGolem golem, Container destination) {
            return false;
        }
    }
}
