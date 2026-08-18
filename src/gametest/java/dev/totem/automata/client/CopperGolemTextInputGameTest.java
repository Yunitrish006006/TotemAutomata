package dev.totem.automata.client;

import dev.totem.automata.menu.CopperGolemMenu;
import dev.totem.automata.menu.CopperGolemMenuOpenData;
import dev.totem.automata.menu.CopperGolemMenuRegistration;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.UUID;

/** Regression coverage for the inventory-key/E text-entry conflict. */
@SuppressWarnings("UnstableApiUsage")
public final class CopperGolemTextInputGameTest implements FabricClientGameTest {
    private static final UUID GOLEM_ID = UUID.fromString("00000000-0000-0000-0000-000000000043");

    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
            singleplayer.getClientLevel().waitForChunksRender();

            context.runOnClient(client -> {
                if (client.player == null) {
                    throw new AssertionError("Client GameTest did not provide a player inventory");
                }
                CopperGolemMenu menu = new CopperGolemMenu(
                        CopperGolemMenuRegistration.TYPE,
                        0,
                        client.player.getInventory(),
                        new CopperGolemMenuOpenData(GOLEM_ID)
                );
                client.setScreenAndShow(new CopperGolemMenuScreen(
                        menu,
                        client.player.getInventory(),
                        Component.translatable("container.deadrecall.copper_wrench.bindings")
                ));
            });

            context.waitForScreen(CopperGolemMenuScreen.class);
            context.waitTicks(2);

            int[] positions = context.computeOnClient(client -> {
                var bounds = CopperGolemMenuPanelLayout.bounds(
                        client.getWindow().getGuiScaledWidth(),
                        client.getWindow().getGuiScaledHeight()
                );
                return new int[]{
                        bounds.x() + 160, bounds.y() + 12,
                        bounds.x() + 40, bounds.y() + 59
                };
            });

            // Open the LLM tab, then focus the API URL EditBox.
            context.getInput().setCursorPos(positions[0], positions[1]);
            context.getInput().pressMouse(GLFW.GLFW_MOUSE_BUTTON_LEFT);
            context.getInput().setCursorPos(positions[2], positions[3]);
            context.getInput().pressMouse(GLFW.GLFW_MOUSE_BUTTON_LEFT);
            context.waitTicks(1);

            context.runOnClient(client -> {
                CopperGolemMenuScreen screen = currentScreen(client.screen,
                        "Copper Golem screen closed before the key regression check");
                EditBox editBox = focusedEditor(screen,
                        "LLM API editor did not receive keyboard focus");
                editBox.setValue("abc");
            });

            // E is the default inventory key. Before the fix, this closes the
            // AbstractContainerScreen instead of remaining inside the editor.
            context.getInput().pressKey(GLFW.GLFW_KEY_E);
            context.waitTicks(1);
            context.runOnClient(client -> {
                CopperGolemMenuScreen screen = currentScreen(client.screen,
                        "Inventory-key E closed the Copper Golem screen while editing text");
                focusedEditor(screen,
                        "Copper Golem editor lost focus after inventory-key E");
            });

            // pressKey intentionally models keyPressed only; Unicode text is a
            // separate client input event, matching the real GUI event split.
            context.getInput().typeChars("e");
            context.waitTicks(1);
            context.runOnClient(client -> {
                CopperGolemMenuScreen screen = currentScreen(client.screen,
                        "Copper Golem screen closed after typing text");
                EditBox editBox = focusedEditor(screen,
                        "Copper Golem editor was no longer focused after typing");
                String value = editBox.getValue();
                if (value.length() != 4 || value.indexOf('e') < 0) {
                    throw new AssertionError("Expected one typed 'e' in the four-character editor value, found '"
                            + value + "'");
                }
                client.setScreenAndShow(null);
            });
        }
    }

    private static CopperGolemMenuScreen currentScreen(Object screen, String failure) {
        if (screen instanceof CopperGolemMenuScreen copperGolemScreen) {
            return copperGolemScreen;
        }
        throw new AssertionError(failure);
    }

    private static EditBox focusedEditor(CopperGolemMenuScreen screen, String failure) {
        if (screen.getFocused() instanceof EditBox editBox && editBox.isFocused()) {
            return editBox;
        }
        throw new AssertionError(failure);
    }
}
