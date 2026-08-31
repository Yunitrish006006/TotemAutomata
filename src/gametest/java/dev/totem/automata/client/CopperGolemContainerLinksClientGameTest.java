package dev.totem.automata.client;

import dev.totem.automata.network.CopperGolemVisualizationPayload;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Visual proof that solid source/destination links retain normal terrain depth testing. */
@SuppressWarnings("UnstableApiUsage")
public final class CopperGolemContainerLinksClientGameTest implements FabricClientGameTest {
    private static final BlockPos LOOK_TARGET = new BlockPos(0, 81, -4);

    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
            singleplayer.getClientLevel().waitForChunksRender();
            singleplayer.getServer().runCommand("fill -4 79 -5 4 79 5 minecraft:stone");
            singleplayer.getServer().runCommand("tp @a 0 80 4");
            singleplayer.getServer().runCommand("clear @a");
            singleplayer.getServer().runCommand("fill -1 80 -2 1 83 -2 minecraft:stone_bricks");
            singleplayer.getServer().runCommand("setblock 0 80 -4 minecraft:copper_block");
            singleplayer.getServer().runCommand("setblock -3 80 -4 minecraft:chest");
            singleplayer.getServer().runCommand("setblock 3 80 -4 minecraft:barrel");
            singleplayer.getServer().runCommand("setblock 3 82 -4 minecraft:barrel");
            context.waitFor(client -> client.player != null
                    && client.player.getZ() > 4.0D
                    && client.player.getY() > 79.0D
                    && client.level.getBlockState(new BlockPos(0, 81, -2)).is(Blocks.STONE_BRICKS)
                    && client.level.getBlockState(new BlockPos(-3, 80, -4)).is(Blocks.CHEST)
                    && client.level.getBlockState(new BlockPos(3, 80, -4)).is(Blocks.BARREL));
            context.getInput().lookAt(LOOK_TARGET);
            context.waitTicks(20);

            CopperGolemVisualizationPayload.PosEntry source = pos(-3, 80, -4, true);
            List<CopperGolemVisualizationPayload.PosEntry> destinations = List.of(
                    pos(3, 80, -4, true),
                    pos(3, 82, -4, false)
            );
            context.runOnClient(client -> {
                try (var ignored = client.levelRenderer.collectPerFrameRenderThreadGizmos()) {
                    CopperGolemVisualizationClient.addContainerLinks(
                            new Vec3(0.5D, 80.9D, -3.5D),
                            source,
                            destinations,
                            "sorting",
                            client.level.dimension().identifier().toString()
                    );
                }
            });
            context.takeScreenshot("totem-automata-container-links-depth-tested");
        }
    }

    private static CopperGolemVisualizationPayload.PosEntry pos(
            int x,
            int y,
            int z,
            boolean available) {
        return new CopperGolemVisualizationPayload.PosEntry(
                "minecraft:overworld",
                x, y, z,
                available
        );
    }
}
