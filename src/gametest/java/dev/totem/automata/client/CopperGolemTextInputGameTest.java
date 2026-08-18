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

            // Use a test-only accessor instead of screen coordinates. The
            // regression under test is key routing once an EditBox is focused,
            // not mouse hit-box layout.
            context.runOnClient(client -> {
                CopperGolemMenuScreenTestAccessor accessor = (CopperGolemMenuScreenTestAccessor) screen;
                accessor.totemAutomata$getUi().tab(CopperGolemMenuUiState.Tab.LLM);
                accessor.totemAutomata$updateEditorVisibility();
                EditBox editBox = accessor.totemAutomata$getApiUrlField();
                editBox.setValue("abc");
                screen.setFocused(editBox);
                editBox.setFocused(true);
                focusedEditor(screen, "LLM API editor did not receive keyboard focus");
            });

            // E is the default inventory key. Before the fix, this closes the
            // AbstractContainerScreen instead of remaining inside the editor.
            context.getInput().pressKey(GLFW.GLFW_KEY_E);
            context.waitForScreen(CopperGolemMenuScreen.class);
            context.runOnClient(client -> focusedEditor(screen,
                    "Copper Golem editor lost focus after inventory-key E"));

            // pressKey intentionally models the key mapping; Unicode text is a
            // separate input event, matching Minecraft's real GUI event split.
            context.getInput().typeChars("e");
            context.waitForScreen(CopperGolemMenuScreen.class);
            context.runOnClient(client -> {
                EditBox editBox = focusedEditor(screen,
                        "Copper Golem editor was no longer focused after typing");
                String value = editBox.getValue();
                if (value.length() != 4 || value.indexOf('e') < 0) {
                    throw new AssertionError("Expected one typed 'e' in the four-character editor value, found '"
                            + value + "'");
                }
                client.setScreenAndShow(null);
            });
            context.waitForScreen(null);
        }
    }

    private static EditBox focusedEditor(CopperGolemMenuScreen screen, String failure) {
        if (screen.getFocused() instanceof EditBox editBox && editBox.isFocused()) {
            return editBox;
        }
        throw new AssertionError(failure);
    }
}
