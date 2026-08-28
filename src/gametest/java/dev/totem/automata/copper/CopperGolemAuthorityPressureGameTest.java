package dev.totem.automata.copper;

import dev.totem.automata.network.CopperGolemModePayload;
import dev.totem.automata.network.CopperGolemOperationPayload;
import dev.totem.automata.network.PersistedCopperGolemPayloadHandler;
import dev.totem.automata.network.UpdateCopperGolemGatheringLlmPayload;
import dev.totem.automata.registry.AutomataRegistries;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/** Regression coverage recovered from the pre-extraction DeadRecall authority pressure suite. */
public final class CopperGolemAuthorityPressureGameTest {
    private static final BlockPos GOLEM_POS = new BlockPos(2, 2, 2);
    private static final int PRESSURE_GOLEM_COUNT = 64;

    @GameTest(maxTicks = 40)
    public void staleRevisionCannotMutateModeRunningOrGatheringLlmState(GameTestHelper helper) {
        CopperGolem golem = spawnCopperGolem(helper, GOLEM_POS);
        ServerPlayer player = createBoundPlayer(helper, golem, GOLEM_POS.offset(1, 0, 0));
        try {
            int initialRevision = initializeAndReadRevision(golem);
            PersistedCopperGolemPayloadHandler handler = new PersistedCopperGolemPayloadHandler((viewer, target) -> { });

            handler.setMode(player, new CopperGolemModePayload(
                    golem.getUUID(), CopperGolemMode.GATHERING.id(), initialRevision));

            CompoundTag accepted = CopperGolemData.readEntityTag(golem);
            int acceptedRevision = revision(accepted);
            require(helper, acceptedRevision == initialRevision + 1,
                    "Accepted mode mutation did not advance the authoritative revision exactly once");
            require(helper, CopperGolemData.mode(accepted) == CopperGolemMode.GATHERING,
                    "Accepted mode mutation did not enter gathering mode");

            handler.setOperation(player, new CopperGolemOperationPayload(
                    golem.getUUID(), true, initialRevision));
            handler.updateGatheringLlm(player, new UpdateCopperGolemGatheringLlmPayload(
                    golem.getUUID(), true, "stale prompt", initialRevision));
            handler.setMode(player, new CopperGolemModePayload(
                    golem.getUUID(), CopperGolemMode.SORTING.id(), initialRevision));

            CompoundTag afterStaleMutations = CopperGolemData.readEntityTag(golem);
            GatheringLlmState.Config gatheringLlm = GatheringLlmState.read(afterStaleMutations);
            require(helper, revision(afterStaleMutations) == acceptedRevision,
                    "A stale payload changed the authoritative revision");
            require(helper, CopperGolemData.mode(afterStaleMutations) == CopperGolemMode.GATHERING,
                    "A stale mode payload changed authoritative state");
            require(helper, !afterStaleMutations.getBooleanOr(CopperGolemData.TAG_TRANSPORT_ENABLED, false),
                    "A stale operation payload changed authoritative running state");
            require(helper, !gatheringLlm.enabled() && gatheringLlm.prompt().isBlank(),
                    "A stale gathering LLM payload changed authoritative state");
            helper.succeed();
        } finally {
            player.discard();
            golem.discard();
        }
    }

    @GameTest(maxTicks = 40)
    public void sameRevisionCompetingPlayersOnlyAllowFirstMutation(GameTestHelper helper) {
        CopperGolem golem = spawnCopperGolem(helper, GOLEM_POS);
        ServerPlayer first = createBoundPlayer(helper, golem, GOLEM_POS.offset(1, 0, 0));
        ServerPlayer second = createBoundPlayer(helper, golem, GOLEM_POS.offset(-1, 0, 0));
        try {
            int sharedRevision = initializeAndReadRevision(golem);
            PersistedCopperGolemPayloadHandler handler = new PersistedCopperGolemPayloadHandler((viewer, target) -> { });

            handler.setOperation(first, new CopperGolemOperationPayload(
                    golem.getUUID(), true, sharedRevision));
            handler.setOperation(second, new CopperGolemOperationPayload(
                    golem.getUUID(), false, sharedRevision));

            CompoundTag tag = CopperGolemData.readEntityTag(golem);
            require(helper, tag.getBooleanOr(CopperGolemData.TAG_TRANSPORT_ENABLED, false),
                    "The second same-revision mutation overwrote the first accepted operation state");
            require(helper, revision(tag) == sharedRevision + 1,
                    "Competing same-revision mutations advanced the revision more than once");
            helper.succeed();
        } finally {
            first.discard();
            second.discard();
            golem.discard();
        }
    }

    @GameTest(maxTicks = 80)
    public void manyTrackedGolemsPruneDiscardedEntries(GameTestHelper helper) {
        CopperGolemController controller = new CopperGolemController();
        Set<UUID> fixtureIds = new HashSet<>();
        CopperGolemBehavior behavior = new CopperGolemBehavior() {
            @Override
            public boolean shouldTrack(CopperGolem golem) {
                return fixtureIds.contains(golem.getUUID()) && golem.isAlive() && !golem.isRemoved();
            }

            @Override
            public void tick(net.minecraft.server.MinecraftServer server,
                             net.minecraft.server.level.ServerLevel level,
                             CopperGolem golem,
                             boolean shouldPruneBindings) {
                // This regression isolates controller bookkeeping under entity pressure.
            }
        };

        List<CopperGolem> golems = new ArrayList<>();
        for (int index = 0; index < PRESSURE_GOLEM_COUNT; index++) {
            int x = 2 + (index % 8) * 2;
            int z = 2 + (index / 8) * 2;
            CopperGolem golem = spawnCopperGolem(helper, new BlockPos(x, 2, z));
            fixtureIds.add(golem.getUUID());
            controller.track(golem);
            golems.add(golem);
        }

        require(helper, trackedCount(controller) == PRESSURE_GOLEM_COUNT,
                "Controller failed to track the 64-golem pressure fixture");

        controller.tick(helper.getLevel().getServer(), behavior);
        require(helper, trackedCount(controller) == PRESSURE_GOLEM_COUNT,
                "Controller dropped live golems during the pressure tick");

        for (int index = 0; index < PRESSURE_GOLEM_COUNT / 2; index++) {
            golems.get(index).discard();
        }
        controller.tick(helper.getLevel().getServer(), behavior);

        require(helper, trackedCount(controller) == PRESSURE_GOLEM_COUNT / 2,
                "Controller retained discarded Copper Golem tracking entries");
        for (CopperGolem golem : golems) {
            controller.untrack(golem);
            golem.discard();
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void inactiveLoadedGolemRemainsTrackedAndTicksAfterActivation(GameTestHelper helper) {
        CopperGolemController controller = new CopperGolemController();
        CopperGolem golem = spawnCopperGolem(helper, GOLEM_POS);
        AtomicBoolean active = new AtomicBoolean();
        AtomicInteger ticks = new AtomicInteger();
        CopperGolemBehavior behavior = new CopperGolemBehavior() {
            @Override public boolean shouldTrack(CopperGolem value) {
                return active.get();
            }

            @Override public void tick(net.minecraft.server.MinecraftServer server,
                                       net.minecraft.server.level.ServerLevel level,
                                       CopperGolem value,
                                       boolean shouldPruneBindings) {
                ticks.incrementAndGet();
            }
        };

        controller.track(golem);
        controller.tick(helper.getLevel().getServer(), behavior);
        require(helper, trackedCount(controller) == 1,
                "An inactive but loaded Copper Golem was removed from event-scoped tracking");
        require(helper, ticks.get() == 0, "Inactive Copper Golem unexpectedly ran managed work");

        active.set(true);
        controller.tick(helper.getLevel().getServer(), behavior);
        require(helper, ticks.get() == 1,
                "A loaded Copper Golem did not become active without a full-entity discovery scan");
        controller.untrack(golem);
        golem.discard();
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void lifecycleEventsTrackAndUntrackLoadedCopperGolem(GameTestHelper helper) {
        CopperGolem golem = spawnCopperGolem(helper, GOLEM_POS);
        require(helper, CopperGolemLifecycleRegistration.isTrackedForTesting(golem.getUUID()),
                "ENTITY_LOAD did not add the Copper Golem to the scheduler");
        UUID golemId = golem.getUUID();
        golem.discard();
        helper.runAfterDelay(1, () -> {
            require(helper, !CopperGolemLifecycleRegistration.isTrackedForTesting(golemId),
                    "ENTITY_UNLOAD did not remove the Copper Golem from the scheduler");
            helper.succeed();
        });
    }

    @GameTest(maxTicks = 40)
    public void persistedTargetSkipsCursorAndExpensiveValidation(GameTestHelper helper) {
        CopperGolem golem = spawnCopperGolem(helper, GOLEM_POS);
        BlockPos target = helper.absolutePos(GOLEM_POS.offset(5, 0, 0));
        CompoundTag tag = CopperGolemData.readEntityTag(golem);
        CopperGolemData.migrate(tag);
        tag.putString(CopperGolemData.TAG_MODE, CopperGolemMode.GATHERING.id());
        tag.putBoolean(CopperGolemData.TAG_TRANSPORT_ENABLED, true);
        GatheringConfiguration.setCorner(tag, helper.getLevel().dimension(), target.offset(-1, -1, -1), false);
        GatheringConfiguration.setCorner(tag, helper.getLevel().dimension(), target.offset(1, 1, 1), true);
        CopperGolemData.writeItemStack(tag, "deadrecall_gathering_tool_stack",
                new ItemStack(Items.IRON_PICKAXE), helper.getLevel().registryAccess());
        tag.putInt(GatheringRuntimeState.TARGET_X, target.getX());
        tag.putInt(GatheringRuntimeState.TARGET_Y, target.getY());
        tag.putInt(GatheringRuntimeState.TARGET_Z, target.getZ());
        GatheringRuntimeState.setActivity(tag, CopperGolemActivity.MOVING_TO_TARGET);
        CopperGolemData.writeEntityTag(golem, tag);

        AtomicInteger cheapChecks = new AtomicInteger();
        AtomicInteger expensiveChecks = new AtomicInteger();
        AtomicInteger targetTicks = new AtomicInteger();
        PersistedGatheringBehavior behavior = new PersistedGatheringBehavior(
                new PersistedGatheringBehavior.WorldOperations() {
                    @Override public boolean hasHome(CopperGolem value, net.minecraft.server.level.ServerLevel level, CompoundTag snapshot) { return true; }
                    @Override public boolean hasFuel(CopperGolem value, net.minecraft.server.level.ServerLevel level, CompoundTag snapshot) { return true; }
                    @Override public boolean hasTargetRules(CopperGolem value, CompoundTag snapshot) { return true; }
                    @Override public boolean isCheaplyValidTarget(CopperGolem value, net.minecraft.server.level.ServerLevel level,
                                                                   CompoundTag snapshot, GatheringScanCursor.Bounds bounds, BlockPos pos) {
                        cheapChecks.incrementAndGet();
                        return true;
                    }
                    @Override public boolean isValidTarget(CopperGolem value, net.minecraft.server.level.ServerLevel level,
                                                            CompoundTag snapshot, BlockPos pos) {
                        expensiveChecks.incrementAndGet();
                        return true;
                    }
                    @Override public void deposit(CopperGolem value, net.minecraft.server.level.ServerLevel level, List<ItemStack> storage) { }
                    @Override public void stop(CopperGolem value) { }
                    @Override public void tickTarget(CopperGolem value, net.minecraft.server.level.ServerLevel level,
                                                     CompoundTag snapshot, BlockPos pos) { targetTicks.incrementAndGet(); }
                }
        );

        CopperGolemBehavior.TickResult result = behavior.tickScheduled(
                helper.getLevel().getServer(), helper.getLevel(), golem, false, 32);
        require(helper, result.scanPositionsInspected() == 0,
                "A persisted target consumed cursor scan budget");
        require(helper, cheapChecks.get() == 1,
                "A persisted target did not receive exactly one cheap validity guard");
        require(helper, expensiveChecks.get() == 0,
                "A persisted target unexpectedly re-ran expensive candidate validation");
        require(helper, targetTicks.get() == 1,
                "The persisted target was not processed after skipping discovery");
        golem.discard();
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void disabledGatheringTransitionsOnceThenKeepsStateUnchanged(GameTestHelper helper) {
        CopperGolem golem = spawnCopperGolem(helper, GOLEM_POS);
        CompoundTag tag = CopperGolemData.readEntityTag(golem);
        CopperGolemData.migrate(tag);
        tag.putString(CopperGolemData.TAG_MODE, CopperGolemMode.GATHERING.id());
        tag.putBoolean(CopperGolemData.TAG_TRANSPORT_ENABLED, false);
        GatheringRuntimeState.setActivity(tag, CopperGolemActivity.MOVING_TO_TARGET);
        CopperGolemData.writeEntityTag(golem, tag);
        golem.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_PICKAXE));

        PersistedCopperGolemRuntime runtime = new PersistedCopperGolemRuntime();
        runtime.tickScheduled(helper.getLevel().getServer(), helper.getLevel(), golem, false, 32);
        CompoundTag afterTransition = CopperGolemData.readEntityTag(golem);
        require(helper, CopperGolemData.activity(afterTransition) == CopperGolemActivity.STOPPED,
                "Disabled gathering did not enter STOPPED");
        require(helper, golem.getMainHandItem().isEmpty(),
                "Disabled gathering did not clear its virtual displayed item once");

        runtime.tickScheduled(helper.getLevel().getServer(), helper.getLevel(), golem, false, 32);
        require(helper, afterTransition.equals(CopperGolemData.readEntityTag(golem)),
                "A stopped gathering Golem mutated persisted state on a later tick");
        golem.discard();
        helper.succeed();
    }

    private static int initializeAndReadRevision(CopperGolem golem) {
        CompoundTag tag = CopperGolemData.readEntityTag(golem);
        CopperGolemData.migrate(tag);
        CopperGolemData.writeEntityTag(golem, tag);
        return revision(tag);
    }

    private static int revision(CompoundTag tag) {
        return tag.getIntOr(CopperGolemData.TAG_REVISION, 0);
    }

    private static ServerPlayer createBoundPlayer(GameTestHelper helper, CopperGolem golem, BlockPos relativePos) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        BlockPos absolute = helper.absolutePos(relativePos);
        player.snapTo(absolute.getX() + 0.5D, absolute.getY(), absolute.getZ() + 0.5D, 0.0F, 0.0F);
        ItemStack wrench = new ItemStack(AutomataRegistries.COPPER_WRENCH);
        require(helper, CopperWrenchSelection.select(wrench, golem.getUUID()),
                "Could not bind the pressure-test Copper Wrench to the target golem");
        player.setItemInHand(InteractionHand.MAIN_HAND, wrench);
        return player;
    }

    private static CopperGolem spawnCopperGolem(GameTestHelper helper, BlockPos relativePos) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(
                Identifier.fromNamespaceAndPath("minecraft", "copper_golem"));
        if (type == null) {
            throw helper.assertionException("Missing minecraft:copper_golem entity type");
        }
        Entity entity = type.create(helper.getLevel(), EntitySpawnReason.COMMAND);
        if (!(entity instanceof CopperGolem golem)) {
            throw helper.assertionException("Could not create a Copper Golem pressure fixture");
        }
        BlockPos absolute = helper.absolutePos(relativePos);
        golem.snapTo(absolute.getX() + 0.5D, absolute.getY(), absolute.getZ() + 0.5D, 0.0F, 0.0F);
        if (!helper.getLevel().addFreshEntity(golem)) {
            throw helper.assertionException("Could not add a Copper Golem pressure fixture");
        }
        return golem;
    }

    @SuppressWarnings("unchecked")
    private static int trackedCount(CopperGolemController controller) {
        try {
            Field trackedField = CopperGolemController.class.getDeclaredField("tracked");
            trackedField.setAccessible(true);
            return ((Map<UUID, ?>) trackedField.get(controller)).size();
        } catch (ReflectiveOperationException exception) {
            throw new RuntimeException("Could not inspect CopperGolemController tracking state", exception);
        }
    }

    private static void require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            throw helper.assertionException(message);
        }
    }
}
