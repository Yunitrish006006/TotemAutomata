package dev.totem.automata.mixin.client;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.inventory.MenuType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

/** Cutover-only access to the vanilla screen factory map. */
@Mixin(MenuScreens.class)
public interface MenuScreensAccessor {
    @Accessor("SCREENS")
    static Map<MenuType<?>, Object> totemAutomata$getScreens() {
        throw new AssertionError("Mixin accessor was not applied");
    }
}
