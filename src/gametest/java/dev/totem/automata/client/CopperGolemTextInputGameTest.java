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

            CopperGolemMenuScreen screen = context.computeOnClient(client -> {
                if (client.player == null) {
                    throw new AssertionError("Client GameTest did not provide a player inventory");
                }
                CopperGolemMenu menu = new CopperGolemMenu(
                        CopperGolemMenuRegistration.TYPE,
                        0,
                        client.player.getInventory(),
                        new CopperGolemMenuOpenData(GOLEM_ID)
                );
                CopperGolemMenuScreen opened = new CopperGolemMenuScreen(
                        menu,
                        client.player.getInventory(),
                        Component.translatable("container.deadrecall.copper_wrench.bindings")
                );
                client.setScreenAndShow(opened);
                return opened;
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
                if (client.screen != screen) {
                    throw new AssertionError("Copper Golem screen closed before the key regression check");
                }
                if (!(screen.getFocused() instanceof EditBox editBox) || !editBox.isFocused()) {
                    throw new AssertionError("LLM API editor did not receive keyboard focus");
                }
                editBox.setValue("abc");
            });

            // E is the default inventory key. Before the fix, this closes the
            // AbstractContainerScreen instead of remaining inside the editor.
            context.getInput().pressKey(GLFW.GLFW_KEY_E);
            context.waitTicks(1);
            context.runOnClient(client -> {
                if (client.screen != screen) {
                    throw new AssertionError("Inventory-key E closed the Copper Golem screen while editing text");
                }
                if (!(screen.getFocused() instanceof EditBox editBox) || !editBox.isFocused()) {
                    throw new AssertionError("Copper Golem editor lost focus after inventory-key E");
                }
            });

            // pressKey intentionally models keyPressed only; Unicode text is a
            // separate client input event, matching the real GUI event split.
            context.getInput().typeChars("e");
            context.waitTicks(1);
            context.runOnClient(client -> {
                if (!(screen.getFocused() instanceof EditBox editBox)) {
                    throw new AssertionError("Copper Golem editor was no longer focused after typing");
                }
                if (!"abce".equals(editBox.getValue())) {
                    throw new AssertionError("Expected editor value 'abce' after typing E, found '"
                            + editBox.getValue() + "'");
                }
                client.setScreenAndShow(null);
            });
        }
    }
}
