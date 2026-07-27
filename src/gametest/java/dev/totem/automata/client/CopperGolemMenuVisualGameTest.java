package dev.totem.automata.client;

import dev.totem.automata.menu.CopperGolemMenu;
import dev.totem.automata.menu.CopperGolemMenuOpenData;
import dev.totem.automata.menu.CopperGolemMenuRegistration;
import dev.totem.automata.network.CopperWrenchBindingsPayload;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.UUID;

/** Captures the external Copper Golem screen before and after its safe snapshot arrives. */
@SuppressWarnings("UnstableApiUsage")
public final class CopperGolemMenuVisualGameTest implements FabricClientGameTest {
    private static final UUID GOLEM_ID = UUID.fromString("00000000-0000-0000-0000-000000000042");

    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
            singleplayer.getClientLevel().waitForChunksRender();

            CopperGolemMenuScreen screen = context.computeOnClient(client -> {
                if (client.player == null) {
                    throw new IllegalStateException("Client GameTest did not provide a player inventory");
                }

                CopperGolemMenu menu = new CopperGolemMenu(
                        CopperGolemMenuRegistration.TYPE,
                        0,
                        client.player.getInventory(),
                        new CopperGolemMenuOpenData(GOLEM_ID));
                CopperGolemMenuScreen openedScreen = new CopperGolemMenuScreen(
                        menu, client.player.getInventory(), Component.literal("Copper Golem"));
                client.setScreenAndShow(openedScreen);
                return openedScreen;
            });
            context.waitForScreen(CopperGolemMenuScreen.class);
            context.waitTicks(2);
            context.takeScreenshot("safe-multi-repo-modularization-automata-menu-before");

            context.runOnClient(client -> screen.acceptSnapshotForVisualTest(snapshot()));
            context.waitTicks(2);
            context.takeScreenshot("safe-multi-repo-modularization-automata-menu-after");
            context.runOnClient(client -> client.setScreenAndShow(null));
        }
    }

    private static CopperWrenchBindingsPayload snapshot() {
        CopperWrenchBindingsPayload.BindingEntry source = new CopperWrenchBindingsPayload.BindingEntry(
                "minecraft:overworld", 10, 64, 10, "minecraft:chest", "minecraft:chest",
                true, true, false, "", 0, 0, List.of(), List.of(), List.of(), List.of());
        CopperWrenchBindingsPayload.BindingEntry destination = new CopperWrenchBindingsPayload.BindingEntry(
                "minecraft:overworld", 16, 64, 10, "minecraft:barrel", "minecraft:barrel",
                true, true, true, "Sort ores into this barrel", 2, 1,
                List.of("minecraft:iron_ingot"), List.of(), List.of("c:ores"), List.of());
        return new CopperWrenchBindingsPayload(
                GOLEM_ID, 7, true, "sorting", "searching",
                "minecraft:coal", 4, 800,
                "minecraft:air", 0, 0, 0,
                "minecraft:air", 0,
                "", "", "", 1,
                source, null, List.of(), false, "", 0, 0,
                List.of(), List.of(), List.of(), List.of(), List.of(destination));
    }
}
