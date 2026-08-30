package dev.totem.automata.client;

import dev.totem.automata.network.CopperGolemVisualizationPayload;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;

/** Visual proof that the production gathering-area outline does not render through terrain. */
@SuppressWarnings("UnstableApiUsage")
public final class CopperGolemGatheringAreaOutlineClientGameTest implements FabricClientGameTest {
    private static final BlockPos LOOK_TARGET = new BlockPos(0, 81, -3);

    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
            singleplayer.getClientLevel().waitForChunksRender();
            singleplayer.getServer().runCommand("fill -2 79 -4 2 79 2 minecraft:stone");
            singleplayer.getServer().runCommand("tp @a 0 80 0");
            singleplayer.getServer().runCommand("fill -1 80 -2 1 83 -2 minecraft:stone_bricks");
            context.waitFor(client -> client.player != null
                    && client.level.getBlockState(new BlockPos(0, 81, -2)).is(Blocks.STONE_BRICKS));
            context.getInput().lookAt(LOOK_TARGET);
            context.waitTicks(3);

            CopperGolemVisualizationPayload.AreaEntry partlyOccludedArea =
                    new CopperGolemVisualizationPayload.AreaEntry(
                            "minecraft:overworld",
                            true, -2, 80, -3,
                            true, 2, 82, -3
                    );
            // The screenshot helper bypasses the outer BEFORE_GIZMOS collector,
            // so submit the production helper into the screenshot frame collector.
            context.runOnClient(client -> {
                try (var ignored = client.levelRenderer.collectPerFrameRenderThreadGizmos()) {
                    CopperGolemVisualizationClient.addGatheringAreaOutline(
                            partlyOccludedArea,
                            client.level.dimension().identifier().toString()
                    );
                }
            });
            context.takeScreenshot("totem-automata-gathering-area-depth-tested");
        }
    }
}
