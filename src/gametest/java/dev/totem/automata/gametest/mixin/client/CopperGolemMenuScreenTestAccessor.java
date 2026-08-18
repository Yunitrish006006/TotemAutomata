package dev.totem.automata.gametest.mixin.client;

import dev.totem.automata.client.CopperGolemMenuScreen;
import dev.totem.automata.client.CopperGolemMenuUiState;
import net.minecraft.client.gui.components.EditBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Test-only access to deterministic Copper Golem editor state. */
@Mixin(CopperGolemMenuScreen.class)
public interface CopperGolemMenuScreenTestAccessor {
    @Accessor("apiUrlField")
    EditBox totemAutomata$getApiUrlField();

    @Accessor("ui")
    CopperGolemMenuUiState totemAutomata$getUi();

    @Invoker("updateEditorVisibility")
    void totemAutomata$updateEditorVisibility();
}
