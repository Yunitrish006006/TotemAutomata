package dev.totem.automata.menu;

import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;

/** Preserved menu registration, activated only by the complete Automata cutover. */
public final class CopperGolemMenuRegistration {
    public static final ExtendedMenuType<CopperGolemMenu, CopperGolemMenuOpenData> TYPE = Registry.register(
            BuiltInRegistries.MENU, CopperGolemMenuIds.COPPER_GOLEM,
            new ExtendedMenuType<>((containerId, inventory, data) -> new CopperGolemMenu(clientType(), containerId, inventory, data),
                    CopperGolemMenuOpenData.STREAM_CODEC));

    private CopperGolemMenuRegistration() { }
    public static void register() { /* class loading registers the preserved type at cutover */ }
    private static MenuType<?> clientType() {
        return BuiltInRegistries.MENU.get(CopperGolemMenuIds.COPPER_GOLEM).map(reference -> reference.value()).orElseThrow();
    }
}
