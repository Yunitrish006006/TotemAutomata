package dev.totem.automata.client;

import dev.totem.automata.network.CopperGolemVisualizationPayload;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Visual proof that solid source/destination links retain normal terrain depth testing. */
@SuppressWarnings("UnstableApiUsage")
public final class CopperGolemContainerLinksClientGameTest implements FabricClientGameTest {
    private static final BlockPos LOOK_TARGET = new BlockPos(0, 81, -4);
    private static final Vec3 STALE_SNAPSHOT_CENTER = new Vec3(40.0D, 70.9D, 40.0D);

    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
            singleplayer.getClientLevel().waitForChunksRender();
            singleplayer.getServer().runCommand("fill -4 79 -5 4 79 5 minecraft:stone");
            singleplayer.getServer().runCommand("tp @a 0 80 4");
            singleplayer.getServer().runCommand("clear @a");
            singleplayer.getServer().runCommand("fill -1 80 -2 1 83 -2 minecraft:stone_bricks");
            singleplayer.getServer().runCommand("setblock -3 80 -4 minecraft:chest");
            singleplayer.getServer().runCommand("setblock 3 80 -4 minecraft:barrel");
            singleplayer.getServer().runCommand("setblock 3 82 -4 minecraft:barrel");
            singleplayer.getServer().runCommand(
                    "summon minecraft:copper_golem 0.5 80 0.5 {NoAI:1b,PersistenceRequired:1b}");
            context.waitFor(client -> client.player != null
                    && client.player.getZ() > 4.0D
                    && client.player.getY() > 79.0D
                    && client.level.getBlockState(new BlockPos(0, 81, -2)).is(Blocks.STONE_BRICKS)
                    && client.level.getBlockState(new BlockPos(-3, 80, -4)).is(Blocks.CHEST)
                    && client.level.getBlockState(new BlockPos(3, 80, -4)).is(Blocks.BARREL)
                    && isGolemNear(client, 0.5D));
            context.runOnClient(client -> assertLiveCenter(client, 0.5D));
            singleplayer.getServer().runCommand(
                    "tp @e[type=minecraft:copper_golem,limit=1] 1.5 80 0.5");
            context.waitFor(client -> isGolemNear(client, 1.5D));
            context.getInput().lookAt(LOOK_TARGET);
            context.waitTicks(20);

            CopperGolemVisualizationPayload.PosEntry source = pos(-3, 80, -4, true);
            List<CopperGolemVisualizationPayload.PosEntry> destinations = List.of(
                    pos(3, 80, -4, true),
                    pos(3, 82, -4, false)
            );
            context.runOnClient(client -> {
                CopperGolem golem = findGolem(client);
                if (golem == null) {
                    throw new IllegalStateException("Client Copper Golem disappeared before line rendering");
                }
                Vec3 golemCenter = CopperGolemVisualizationClient.currentGolemCenter(
                        client.level,
                        golem.getUUID(),
                        client.getDeltaTracker().getGameTimeDeltaPartialTick(!client.isPaused()),
                        STALE_SNAPSHOT_CENTER
                );
                assertNear(golemCenter.x, 1.5D, "Live line origin did not follow the moved Copper Golem");
                try (var ignored = client.levelRenderer.collectPerFrameRenderThreadGizmos()) {
                    CopperGolemVisualizationClient.addContainerLinks(
                            golemCenter,
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

    private static void assertLiveCenter(Minecraft client, double expectedX) {
        CopperGolem golem = findGolem(client);
        if (golem == null) {
            throw new IllegalStateException("Missing client Copper Golem for live line origin test");
        }
        Vec3 center = CopperGolemVisualizationClient.currentGolemCenter(
                client.level,
                golem.getUUID(),
                1.0F,
                STALE_SNAPSHOT_CENTER
        );
        assertNear(center.x, expectedX, "Line origin used the stale server snapshot");
        assertNear(center.y, golem.getY() + 0.9D, "Line origin used the wrong Copper Golem height");
        assertNear(center.z, golem.getZ(), "Line origin used the wrong Copper Golem depth");
    }

    private static boolean isGolemNear(Minecraft client, double expectedX) {
        CopperGolem golem = findGolem(client);
        return golem != null && Math.abs(golem.getX() - expectedX) < 0.05D;
    }

    private static CopperGolem findGolem(Minecraft client) {
        if (client.level == null) {
            return null;
        }
        for (Entity entity : client.level.entitiesForRendering()) {
            if (entity instanceof CopperGolem golem) {
                return golem;
            }
        }
        return null;
    }

    private static void assertNear(double actual, double expected, String message) {
        if (Math.abs(actual - expected) > 0.05D) {
            throw new IllegalStateException(message + ": expected " + expected + ", got " + actual);
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
