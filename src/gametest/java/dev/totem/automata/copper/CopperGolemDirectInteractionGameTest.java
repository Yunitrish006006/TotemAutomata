package dev.totem.automata.copper;

import dev.totem.automata.registry.AutomataRegistries;
import dev.totem.automata.network.PersistedCopperGolemSnapshotSender;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.ItemStack;

import java.util.concurrent.atomic.AtomicReference;

/** Verifies that a Wrench right-click opens the Copper Golem menu without Shift. */
public final class CopperGolemDirectInteractionGameTest {
    private static final BlockPos SOURCE_CHEST_POS = new BlockPos(3, 2, 3);

    @GameTest(maxTicks = 20)
    public void wrenchRightClickOpensTheCopperGolemMenuWithoutShift(GameTestHelper helper) {
        CopperGolem golem = spawnCopperGolem(helper);
        if (golem == null) {
            helper.fail("Could not spawn a Copper Golem for the direct interaction test");
            return;
        }

        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(AutomataRegistries.COPPER_WRENCH));
        AtomicReference<ServerPlayer> openedFor = new AtomicReference<>();
        AtomicReference<CopperGolem> openedGolem = new AtomicReference<>();
        PersistedCopperWrenchInteractionAuthority authority = new PersistedCopperWrenchInteractionAuthority(
                (viewer, target) -> {
                    openedFor.set(viewer);
                    openedGolem.set(target);
                });

        InteractionResult result = authority.useEntity(player, helper.getLevel(), InteractionHand.MAIN_HAND, golem);
        if (result != InteractionResult.SUCCESS || openedFor.get() != player || openedGolem.get() != golem) {
            helper.fail("Wrench right-click did not open the Copper Golem menu without Shift");
            return;
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void wrenchBindsCopperChestAsSourceAndSendsItsIconData(GameTestHelper helper) {
        CopperGolem golem = spawnCopperGolem(helper);
        if (golem == null) {
            helper.fail("Could not spawn a Copper Golem for the source binding test");
            return;
        }
        Block copperChest = BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("minecraft", "copper_chest"));
        if (copperChest == null || copperChest == Blocks.AIR) {
            helper.fail("Missing minecraft:copper_chest block");
            return;
        }
        helper.setBlock(SOURCE_CHEST_POS, copperChest);
        if (!(helper.getLevel().getBlockEntity(helper.absolutePos(SOURCE_CHEST_POS)) instanceof net.minecraft.world.Container)) {
            helper.fail("Minecraft copper chest did not provide a container block entity");
            return;
        }
        if (!helper.getLevel().getBlockState(helper.absolutePos(SOURCE_CHEST_POS)).is(BlockTags.COPPER_CHESTS)) {
            helper.fail("Minecraft copper chest was not recognized by the copper chest block tag");
            return;
        }

        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(AutomataRegistries.COPPER_WRENCH));
        PersistedCopperWrenchInteractionAuthority authority = new PersistedCopperWrenchInteractionAuthority((viewer, target) -> { });
        CopperWrenchSelection.select(player.getMainHandItem(), golem.getUUID());
        InteractionResult result = authority.useBlock(player, helper.getLevel(), InteractionHand.MAIN_HAND,
                helper.absolutePos(SOURCE_CHEST_POS));
        if (result != InteractionResult.SUCCESS) {
            helper.fail("Copper wrench source binding did not consume the copper chest interaction");
            return;
        }
        var persistedSource = SortingBindingService.getSourceContainer(CopperGolemData.readEntityTag(golem));
        if (persistedSource.filter(binding -> binding.containerPos().equals(helper.absolutePos(SOURCE_CHEST_POS))).isEmpty()) {
            helper.fail("Expected " + helper.absolutePos(SOURCE_CHEST_POS) + ", found " + persistedSource.orElse(null));
            return;
        }

        var source = new PersistedCopperGolemSnapshotSender().snapshot(player, golem).sourceContainer();
        if (source == null || !"minecraft:copper_chest".equals(source.blockId())
                || !"minecraft:copper_chest".equals(source.itemId()) || !source.available()) {
            helper.fail("Source binding did not supply the copper chest icon data to the menu");
            return;
        }
        helper.succeed();
    }

    static CopperGolem spawnCopperGolem(GameTestHelper helper) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(
                Identifier.fromNamespaceAndPath("minecraft", "copper_golem"));
        if (type == null) {
            return null;
        }
        Entity entity = type.create(helper.getLevel(), EntitySpawnReason.COMMAND);
        if (!(entity instanceof CopperGolem golem)) {
            return null;
        }
        BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
        golem.snapTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0.0F, 0.0F);
        return helper.getLevel().addFreshEntity(golem) ? golem : null;
    }
}
