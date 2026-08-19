package dev.totem.automata.copper;

import dev.totem.excavation.registry.ExcavationItems;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/** Ensures Hammer gathering does not fall back to a single-drop-kind backpack. */
public final class CopperGolemHammerMixedStorageGameTest {
    private static final String TOOL = "deadrecall_gathering_tool_stack";

    @GameTest(maxTicks = 40)
    public void sequentialHammerBreaksKeepDifferentDropKinds(GameTestHelper helper) {
        CopperGolem golem = CopperGolemDirectInteractionGameTest.spawnCopperGolem(helper);
        require(helper, golem != null, "Could not spawn Copper Golem for mixed Hammer storage");
        ServerLevel level = helper.getLevel();
        ServerPlayer operator = helper.makeMockServerPlayerInLevel();
        BlockPos planksRelative = new BlockPos(3, 2, 3);
        BlockPos stoneRelative = new BlockPos(4, 2, 3);
        BlockPos planks = helper.absolutePos(planksRelative);
        BlockPos stone = helper.absolutePos(stoneRelative);
        helper.setBlock(planksRelative, Blocks.OAK_PLANKS);
        helper.setBlock(stoneRelative, Blocks.STONE);

        var tag = CopperGolemData.readEntityTag(golem);
        CopperGolemData.writeItemStack(tag, TOOL, new ItemStack(ExcavationItems.NETHERITE_HAMMER), level.registryAccess());
        GatheringStorage.write(tag, java.util.List.of(), level.registryAccess());
        CopperGolemFuelService.writeFuelStack(tag, new ItemStack(Items.COAL), level);
        CopperGolemData.writeEntityTag(golem, tag);

        try {
            require(helper, GatheringBlockBreaker.breakTarget(golem, level, operator, planks) == GatheringBlockBreaker.Result.BROKEN,
                    "Hammer rejected the first mixed-storage target");
            require(helper, GatheringBlockBreaker.breakTarget(golem, level, operator, stone) == GatheringBlockBreaker.Result.BROKEN,
                    "Hammer rejected the second target merely because its drop kind differed");

            var storage = GatheringStorage.read(CopperGolemData.readEntityTag(golem), level.registryAccess());
            require(helper, GatheringStorage.totalCount(storage) == 2, "Hammer mixed storage did not retain both drops");
            require(helper, storage.stream().anyMatch(stack -> stack.is(Items.OAK_PLANKS)), "Oak plank drop was missing");
            require(helper, storage.stream().anyMatch(stack -> stack.is(Items.COBBLESTONE)), "Cobblestone drop was missing");
            helper.succeed();
        } finally {
            operator.discard();
        }
    }

    private static void require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) throw helper.assertionException(message);
    }
}
