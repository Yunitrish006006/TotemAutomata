package dev.totem.automata.mixin.client;

import dev.totem.automata.client.CopperGolemMenuScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keeps the inventory key available as text while editing Copper Golem fields. */
@Mixin(AbstractContainerScreen.class)
public abstract class CopperGolemTextInputMixin {
    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void totemAutomata$keepInventoryKeyInsideFocusedEditor(
            KeyEvent event,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!((Object) this instanceof CopperGolemMenuScreen screen)) {
            return;
        }
        if (screen.getFocused() instanceof EditBox editBox
                && editBox.isFocused()
                && Minecraft.getInstance().options.keyInventory.matches(event)) {
            // The actual character is delivered separately through charTyped.
            // Consuming only keyPressed prevents AbstractContainerScreen from
            // interpreting the same E press as "close inventory".
            cir.setReturnValue(true);
        }
    }
}
