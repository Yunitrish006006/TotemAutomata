package dev.totem.automata.client;

import dev.totem.automata.menu.CopperGolemMenu;
import dev.totem.automata.menu.CopperGolemMenuOpenData;
import dev.totem.automata.menu.CopperGolemMenuRegistration;
import dev.totem.automata.network.CopperWrenchBindingsPayload;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

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
            context.runOnClient(client -> client.player.getInventory().setItem(0, new ItemStack(Items.IRON_INGOT)));
            context.waitTicks(2);
            context.runOnClient(client -> screen.selectBindingForVisualTest(0));
            context.waitTicks(2);
            context.takeScreenshot("automata-menu-en-us-after");
            context.getInput().setCursorPos(560, 176);
            context.getInput().pressMouse(0);
            context.waitTicks(2);
            context.getInput().setCursorPos(800, 400);
            context.waitTicks(1);
            context.takeScreenshot("automata-menu-en-us-manual-filter");
            captureOverflowFilters(context, screen, "automata-menu-en-us-manual-filter-scrolled");
            context.getInput().setCursorPos(276, 420);
            context.getInput().holdMouse(0);
            context.waitTicks(1);
            context.getInput().setCursorPos(276, 176);
            context.getInput().releaseMouse(0);
            context.waitTicks(2);
            context.takeScreenshot("automata-menu-en-us-manual-filter-dragged");
            context.getInput().setCursorPos(276, 140);
            context.getInput().pressMouse(0);
            context.waitTicks(2);
            context.takeScreenshot("automata-menu-en-us-manual-filter-text-entry");
            context.getInput().setCursorPos(300, 195);
            context.getInput().pressMouse(0);
            context.waitTicks(1);
            context.getInput().setCursorPos(112, 188);
            context.waitTicks(2);
            context.takeScreenshot("automata-menu-en-us-hover-tooltip");
            context.runOnClient(client -> screen.acceptSnapshotForVisualTest(gatheringSnapshot()));
            context.waitTicks(2);
            context.getInput().setCursorPos(800, 400);
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
            context.getInput().setCursorPos(560, 176);
            context.getInput().pressMouse(0);
            context.waitTicks(2);
            context.getInput().setCursorPos(800, 400);
            context.waitTicks(1);
            context.takeScreenshot("automata-menu-zh-tw-manual-filter");
            captureOverflowFilters(context, traditionalChineseScreen,
                    "automata-menu-zh-tw-manual-filter-scrolled");
            context.getInput().setCursorPos(276, 420);
            context.getInput().holdMouse(0);
            context.waitTicks(1);
            context.getInput().setCursorPos(276, 176);
            context.getInput().releaseMouse(0);
            context.waitTicks(2);
            context.takeScreenshot("automata-menu-zh-tw-manual-filter-dragged");
            context.getInput().setCursorPos(276, 140);
            context.getInput().pressMouse(0);
            context.waitTicks(2);
            context.takeScreenshot("automata-menu-zh-tw-manual-filter-text-entry");
            context.runOnClient(client -> client.setScreenAndShow(null));

            selectLanguage(context, "es_es", "Origen: Example");
            CopperGolemMenuScreen spanishScreen = openScreen(context);
            context.waitForScreen(CopperGolemMenuScreen.class);
            context.waitTicks(2);
            context.takeScreenshot("automata-menu-es-es-before");
            context.runOnClient(client -> spanishScreen.acceptSnapshotForVisualTest(snapshot()));
            context.runOnClient(client -> spanishScreen.selectBindingForVisualTest(0));
            context.waitTicks(2);
            context.takeScreenshot("automata-menu-es-es-after");
            context.runOnClient(client -> client.setScreenAndShow(null));

            VanillaCopperGolemBackpackPrototypeScreen prototypeScreen = context.computeOnClient(client -> {
                VanillaCopperGolemBackpackPrototypeScreen prototype = new VanillaCopperGolemBackpackPrototypeScreen();
                client.setScreenAndShow(prototype);
                return prototype;
            });
            context.waitForScreen(VanillaCopperGolemBackpackPrototypeScreen.class);
            context.waitTicks(2);
            context.takeScreenshot("automata-golem-backpack-prototype");
            context.getInput().setCursorPos(524, 42);
            context.getInput().pressMouse(0);
            context.waitTicks(2);
            context.getInput().setCursorPos(800, 400);
            context.waitTicks(1);
            context.takeScreenshot("automata-golem-backpack-sorting");
            for (int targetCount = 1; targetCount <= 5; targetCount++) {
                int boundTargetCount = targetCount;
                context.runOnClient(client -> prototypeScreen.setSortingTargetCountForVisualTest(boundTargetCount));
                context.waitTicks(1);
                context.takeScreenshot("automata-golem-backpack-sorting-binding-count-" + targetCount);
            }
            context.getInput().setCursorPos(560, 112);
            context.getInput().pressMouse(0);
            context.waitTicks(2);
            context.takeScreenshot("automata-golem-backpack-sorting-cache");
            context.getInput().setCursorPos(524, 42);
            context.getInput().pressMouse(0);
            context.waitTicks(2);
            context.getInput().setCursorPos(574, 44);
            context.waitTicks(2);
            context.takeScreenshot("automata-golem-backpack-icon-tooltip");
            context.getInput().setCursorPos(374, 104);
            context.getInput().pressMouse(0);
            context.waitTicks(2);
            context.takeScreenshot("automata-golem-backpack-targets");
            context.getInput().setCursorPos(574, 44);
            context.getInput().pressMouse(0);
            context.waitTicks(2);
            context.takeScreenshot("automata-golem-backpack-llm");
            context.getInput().setCursorPos(554, 98);
            context.getInput().pressMouse(0);
            context.waitTicks(2);
            context.takeScreenshot("automata-golem-backpack-llm-disabled");
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
            assertClientToolSlotPolicy(menu);
            CopperGolemMenuScreen screen = new CopperGolemMenuScreen(
                    menu, client.player.getInventory(),
                    Component.translatable("container.deadrecall.copper_wrench.bindings"));
            client.setScreenAndShow(screen);
            return screen;
        });
    }

    /**
     * The client menu is reconstructed from opening data and therefore has no
     * server authority instance. Checking its gathering-tool policy prevents
     * an item drag from dereferencing that authority.
     */
    private static void assertClientToolSlotPolicy(CopperGolemMenu menu) {
        if (!menu.canPlaceGatheringTool(new ItemStack(Items.IRON_PICKAXE))) {
            throw new IllegalStateException("Client Copper Golem tool slot rejected an iron pickaxe");
        }
        if (menu.canPlaceGatheringTool(new ItemStack(Items.STICK))) {
            throw new IllegalStateException("Client Copper Golem tool slot accepted a stick");
        }
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

    private static void captureOverflowFilters(
            ClientGameTestContext context,
            CopperGolemMenuScreen screen,
            String screenshotName) {
        context.runOnClient(client -> {
            screen.acceptSnapshotForVisualTest(overflowSnapshot());
            screen.selectBindingForVisualTest(0);
        });
        context.waitTicks(2);
        context.runOnClient(client -> {
            var bounds = CopperGolemMenuPanelLayout.bounds(
                    client.getWindow().getGuiScaledWidth(), client.getWindow().getGuiScaledHeight());
            screen.mouseScrolled(bounds.x() + 20, bounds.y() + 80, 0, -1);
            screen.mouseScrolled(bounds.x() + 106, bounds.y() + 80, 0, -1);
            if (screen.filterScrollForVisualTest(true) != 4
                    || screen.filterScrollForVisualTest(false) != 4) {
                throw new IllegalStateException("Allow and deny filter lists did not scroll independently by one row");
            }
        });
        context.waitTicks(2);
        context.takeScreenshot(screenshotName);
        context.runOnClient(client -> {
            screen.acceptSnapshotForVisualTest(snapshot());
            screen.selectBindingForVisualTest(0);
        });
        context.waitTicks(2);
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
                "minecraft:nether_star", 1, 800, true,
                "minecraft:air", 0, 0, 0,
                "minecraft:air", 0,
                "", "", "", 0,
                source, null, List.of(), false, "", 0, 0,
                List.of(), List.of(), List.of(), List.of(), List.of(destination));
    }

    private static CopperWrenchBindingsPayload overflowSnapshot() {
        CopperWrenchBindingsPayload.BindingEntry source = new CopperWrenchBindingsPayload.BindingEntry(
                "minecraft:overworld", 10, 64, 10, "minecraft:copper_chest", "minecraft:copper_chest",
                true, true, false, "", 0, 0, List.of(), List.of(), List.of(), List.of());
        CopperWrenchBindingsPayload.BindingEntry destination = new CopperWrenchBindingsPayload.BindingEntry(
                "minecraft:overworld", 16, 64, 10, "minecraft:barrel", "minecraft:barrel",
                true, true, false, "", 22, 0,
                List.of(
                        "minecraft:coal", "minecraft:iron_ingot", "minecraft:gold_ingot", "minecraft:diamond",
                        "minecraft:emerald", "minecraft:redstone", "minecraft:lapis_lazuli", "minecraft:quartz",
                        "minecraft:copper_ingot", "minecraft:amethyst_shard", "minecraft:blaze_rod", "minecraft:ender_pearl"),
                List.of(
                        "minecraft:rotten_flesh", "minecraft:spider_eye", "minecraft:poisonous_potato", "minecraft:bone",
                        "minecraft:string", "minecraft:gunpowder", "minecraft:gravel", "minecraft:dirt",
                        "minecraft:sand", "minecraft:cobblestone"),
                List.of(), List.of());
        return new CopperWrenchBindingsPayload(
                GOLEM_ID, 8, true, "sorting", "searching",
                "minecraft:nether_star", 1, 800, true,
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
                "minecraft:coal", 4, 800, false,
                "totem:excavation/diamond_hammer", 1, 12, 1561,
                "minecraft:copper_ore", 6,
                "", "", "", 0,
                source, null, List.of("minecraft:copper_ore"), true, "", 1, 1,
                List.of("minecraft:raw_copper"), List.of("minecraft:stone"),
                List.of("minecraft:mineable/pickaxe"), List.of("minecraft:logs"), List.of());
    }
}
