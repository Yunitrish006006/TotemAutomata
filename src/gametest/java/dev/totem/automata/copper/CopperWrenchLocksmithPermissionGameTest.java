package dev.totem.automata.copper;

import dev.totem.automata.registry.AutomataRegistries;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.concurrent.atomic.AtomicInteger;

/** Ordering coverage for player-aware Locksmith checks around Wrench binding. */
public final class CopperWrenchLocksmithPermissionGameTest {
    private static final BlockPos COPPER_SOURCE = new BlockPos(3, 2, 3);
    private static final BlockPos DESTINATION = new BlockPos(4, 2, 3);

    @GameTest(maxTicks = 40)
    public void deniedBindingsMutateNeitherStateOperatorCriterionNorVisuals(GameTestHelper helper) {
        Block copperChest = copperChest(helper);
        if (copperChest == null) return;
        helper.setBlock(COPPER_SOURCE, copperChest);
        helper.setBlock(DESTINATION, Blocks.BARREL);

        assertDenied(helper, CopperGolemMode.SORTING, COPPER_SOURCE, true);
        assertDenied(helper, CopperGolemMode.SORTING, DESTINATION, false);
        assertDenied(helper, CopperGolemMode.GATHERING, COPPER_SOURCE, false);
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void allowedBindingsUseCorrectPermissionAndRememberOperator(GameTestHelper helper) {
        Block copperChest = copperChest(helper);
        if (copperChest == null) return;
        helper.setBlock(COPPER_SOURCE, copperChest);
        helper.setBlock(DESTINATION, Blocks.BARREL);

        assertAllowed(helper, CopperGolemMode.SORTING, COPPER_SOURCE, true);
        assertAllowed(helper, CopperGolemMode.SORTING, DESTINATION, false);
        assertAllowed(helper, CopperGolemMode.GATHERING, COPPER_SOURCE, false);
        helper.succeed();
    }

    private static void assertDenied(
            GameTestHelper helper,
            CopperGolemMode mode,
            BlockPos relativePosition,
            boolean expectsExtract
    ) {
        CopperGolem golem = configuredGolem(helper, mode);
        if (golem == null) return;
        ServerPlayer previousOperator = helper.makeMockServerPlayerInLevel();
        GatheringOperator.remember(golem, previousOperator);
        CompoundTag before = CopperGolemData.readEntityTag(golem).copy();

        ServerPlayer actor = wrenchPlayer(helper, golem);
        AtomicInteger criteria = new AtomicInteger();
        AtomicInteger visuals = new AtomicInteger();
        RecordingAccess access = new RecordingAccess(false);
        PersistedCopperWrenchInteractionAuthority authority =
                new PersistedCopperWrenchInteractionAuthority(
                        (viewer, target) -> { },
                        ignored -> criteria.incrementAndGet(),
                        access,
                        (level, target, position) -> visuals.incrementAndGet());
        InteractionResult result = authority.useBlock(
                actor, helper.getLevel(), InteractionHand.MAIN_HAND,
                helper.absolutePos(relativePosition));

        require(helper, result == InteractionResult.SUCCESS,
                "Denied Wrench binding did not consume the protected-container gesture");
        require(helper, before.equals(CopperGolemData.readEntityTag(golem)),
                "Denied Wrench binding mutated Copper Golem state");
        require(helper, GatheringOperator.operatorId(golem)
                        .filter(previousOperator.getUUID()::equals).isPresent(),
                "Denied Wrench binding replaced the last operator");
        require(helper, criteria.get() == 0, "Denied Wrench binding triggered its criterion");
        require(helper, visuals.get() == 0, "Denied Wrench binding emitted path visuals");
        require(helper, access.extractCalls == (expectsExtract ? 1 : 0)
                        && access.insertCalls == (expectsExtract ? 0 : 1),
                "Wrench binding checked the wrong Locksmith operation");
        golem.discard();
        actor.discard();
        previousOperator.discard();
    }

    private static void assertAllowed(
            GameTestHelper helper,
            CopperGolemMode mode,
            BlockPos relativePosition,
            boolean expectsExtract
    ) {
        CopperGolem golem = configuredGolem(helper, mode);
        if (golem == null) return;
        ServerPlayer actor = wrenchPlayer(helper, golem);
        AtomicInteger criteria = new AtomicInteger();
        AtomicInteger visuals = new AtomicInteger();
        RecordingAccess access = new RecordingAccess(true);
        PersistedCopperWrenchInteractionAuthority authority =
                new PersistedCopperWrenchInteractionAuthority(
                        (viewer, target) -> { },
                        ignored -> criteria.incrementAndGet(),
                        access,
                        (level, target, position) -> visuals.incrementAndGet());
        InteractionResult result = authority.useBlock(
                actor, helper.getLevel(), InteractionHand.MAIN_HAND,
                helper.absolutePos(relativePosition));
        CompoundTag after = CopperGolemData.readEntityTag(golem);

        require(helper, result == InteractionResult.SUCCESS,
                "Allowed Wrench binding did not consume the gesture");
        require(helper, GatheringOperator.operatorId(golem).filter(actor.getUUID()::equals).isPresent(),
                "Allowed Wrench binding did not remember its operator");
        require(helper, criteria.get() == 1, "Allowed Wrench binding did not trigger its criterion once");
        require(helper, visuals.get() == 1, "Allowed Wrench binding did not emit one path visual");
        require(helper, access.extractCalls == (expectsExtract ? 1 : 0)
                        && access.insertCalls == (expectsExtract ? 0 : 1),
                "Allowed Wrench binding checked the wrong Locksmith operation");
        if (relativePosition.equals(DESTINATION)) {
            require(helper, SortingBindingService.getBindings(after).stream()
                            .anyMatch(binding -> binding.containerPos().equals(helper.absolutePos(relativePosition))),
                    "Allowed sorting destination was not persisted");
        } else {
            require(helper, SortingBindingService.getSourceContainer(after)
                            .filter(binding -> binding.containerPos().equals(helper.absolutePos(relativePosition)))
                            .isPresent(),
                    "Allowed source/home binding was not persisted");
        }
        golem.discard();
        actor.discard();
    }

    private static CopperGolem configuredGolem(GameTestHelper helper, CopperGolemMode mode) {
        CopperGolem golem = CopperGolemDirectInteractionGameTest.spawnCopperGolem(helper);
        if (golem == null) {
            helper.fail("Could not spawn a Copper Golem for Wrench permission coverage");
            return null;
        }
        CompoundTag tag = CopperGolemData.readEntityTag(golem);
        tag.putString(CopperGolemData.TAG_MODE, mode.id());
        CopperGolemData.writeEntityTag(golem, tag);
        return golem;
    }

    private static ServerPlayer wrenchPlayer(GameTestHelper helper, CopperGolem golem) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        ItemStack wrench = new ItemStack(AutomataRegistries.COPPER_WRENCH);
        CopperWrenchSelection.select(wrench, golem.getUUID());
        player.setItemInHand(InteractionHand.MAIN_HAND, wrench);
        return player;
    }

    private static Block copperChest(GameTestHelper helper) {
        Block block = BuiltInRegistries.BLOCK.getValue(
                Identifier.fromNamespaceAndPath("minecraft", "copper_chest"));
        if (block == null || block == Blocks.AIR) {
            helper.fail("Missing minecraft:copper_chest block");
            return null;
        }
        return block;
    }

    private static void require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) helper.fail(message);
    }

    private static final class RecordingAccess
            implements PersistedCopperWrenchInteractionAuthority.ContainerBindingAccess {
        private final boolean allowed;
        private int extractCalls;
        private int insertCalls;

        private RecordingAccess(boolean allowed) {
            this.allowed = allowed;
        }

        @Override
        public boolean mayExtract(ServerPlayer player, ServerLevel level, BlockPos position) {
            extractCalls++;
            return allowed;
        }

        @Override
        public boolean mayInsert(ServerPlayer player, ServerLevel level, BlockPos position) {
            insertCalls++;
            return allowed;
        }
    }
}
