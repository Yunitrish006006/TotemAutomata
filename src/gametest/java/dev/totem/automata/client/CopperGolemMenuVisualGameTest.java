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
            selectLanguage(context, "en_us", "Source: Example");

            CopperGolemMenuScreen screen = openScreen(context);
            context.waitForScreen(CopperGolemMenuScreen.class);
            context.waitTicks(2);
            context.takeScreenshot("automata-menu-en-us-before");

            context.runOnClient(client -> screen.acceptSnapshotForVisualTest(snapshot()));
            context.waitTicks(2);
            context.runOnClient(client -> screen.selectBindingForVisualTest(0));
            context.waitTicks(2);
            context.takeScreenshot("automata-menu-en-us-after");
            context.getInput().setCursorPos(112, 188);
            context.waitTicks(2);
            context.takeScreenshot("automata-menu-en-us-hover-tooltip");
            context.runOnClient(client -> screen.acceptSnapshotForVisualTest(gatheringSnapshot()));
            context.waitTicks(2);
            context.takeScreenshot("automata-menu-en-us-gathering-icons");
            context.runOnClient(client -> client.setScreenAndShow(null));

            selectLanguage(context, "zh_tw", "來源：Example");
            CopperGolemMenuScreen traditionalChineseScreen = openScreen(context);
            context.waitForScreen(CopperGolemMenuScreen.class);
            context.waitTicks(2);
            context.takeScreenshot("automata-menu-zh-tw-before");
            context.runOnClient(client -> traditionalChineseScreen.acceptSnapshotForVisualTest(snapshot()));
            context.waitTicks(2);
            context.runOnClient(client -> traditionalChineseScreen.selectBindingForVisualTest(0));
            context.waitTicks(2);
            context.takeScreenshot("automata-menu-zh-tw-after");
            context.runOnClient(client -> client.setScreenAndShow(null));
        }
    }

    private static CopperGolemMenuScreen openScreen(ClientGameTestContext context) {
        return context.computeOnClient(client -> {
            if (client.player == null) {
                throw new IllegalStateException("Client GameTest did not provide a player inventory");
            }
            CopperGolemMenu menu = new CopperGolemMenu(
                    CopperGolemMenuRegistration.TYPE,
                    0,
                    client.player.getInventory(),
                    new CopperGolemMenuOpenData(GOLEM_ID));
            CopperGolemMenuScreen screen = new CopperGolemMenuScreen(
                    menu, client.player.getInventory(),
                    Component.translatable("container.deadrecall.copper_wrench.bindings"));
            client.setScreenAndShow(screen);
            return screen;
        });
    }

    private static void selectLanguage(ClientGameTestContext context, String languageCode, String expectedSourceLabel) {
        context.runOnClient(client -> {
            client.options.languageCode = languageCode;
            client.getLanguageManager().setSelected(languageCode);
            client.getLanguageManager().onResourceManagerReload(client.getResourceManager());
        });
        String sourceLabel = context.computeOnClient(client -> Component.translatable(
                "message.deadrecall.copper_wrench.ui_source", Component.literal("Example")).getString());
        if (!expectedSourceLabel.equals(sourceLabel)) {
            throw new IllegalStateException("Expected " + languageCode + " source label '" + expectedSourceLabel
                    + "' but found '" + sourceLabel + "'");
        }
    }

    private static CopperWrenchBindingsPayload snapshot() {
        CopperWrenchBindingsPayload.BindingEntry source = new CopperWrenchBindingsPayload.BindingEntry(
                "minecraft:overworld", 10, 64, 10, "minecraft:copper_chest", "minecraft:copper_chest",
                true, true, false, "", 0, 0, List.of(), List.of(), List.of(), List.of());
        CopperWrenchBindingsPayload.BindingEntry destination = new CopperWrenchBindingsPayload.BindingEntry(
                "minecraft:overworld", 16, 64, 10, "minecraft:barrel", "minecraft:barrel",
                true, true, false, "", 1, 0,
                List.of(), List.of("minecraft:diamond"), List.of(), List.of());
        return new CopperWrenchBindingsPayload(
                GOLEM_ID, 7, true, "sorting", "searching",
                "minecraft:coal", 4, 800,
                "minecraft:air", 0, 0, 0,
                "minecraft:air", 0,
                "", "", "", 0,
                source, null, List.of(), false, "", 0, 0,
                List.of(), List.of(), List.of(), List.of(), List.of(destination));
    }

    private static CopperWrenchBindingsPayload gatheringSnapshot() {
        CopperWrenchBindingsPayload.BindingEntry source = new CopperWrenchBindingsPayload.BindingEntry(
                "minecraft:overworld", 10, 64, 10, "minecraft:copper_chest", "minecraft:copper_chest",
                true, true, false, "", 0, 0, List.of(), List.of(), List.of(), List.of());
        return new CopperWrenchBindingsPayload(
                GOLEM_ID, 8, true, "gathering", "working",
                "minecraft:coal", 4, 800,
                "minecraft:iron_pickaxe", 1, 12, 250,
                "minecraft:copper_ore", 6,
                "", "", "", 0,
                source, null, List.of("minecraft:copper_ore"), true, "", 1, 1,
                List.of("minecraft:raw_copper"), List.of("minecraft:stone"),
                List.of("minecraft:mineable/pickaxe"), List.of("minecraft:logs"), List.of());
    }
}
