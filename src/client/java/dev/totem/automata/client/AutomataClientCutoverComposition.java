package dev.totem.automata.client;

import dev.totem.automata.menu.CopperGolemMenu;
import dev.totem.automata.menu.CopperGolemMenuRegistration;
import dev.totem.automata.mixin.client.MenuScreensAccessor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.lang.reflect.Proxy;

/**
 * Single client activation point for the standalone Automata module.
 */
public final class AutomataClientCutoverComposition {
    private static boolean activated;

    private AutomataClientCutoverComposition() {
    }

    public static synchronized void activate() {
        if (activated) {
            return;
        }
        CopperGolemMenuClientCutover.install();
        CopperGolemVisualizationClient.initialize();
        registerCopperGolemScreen();
        activated = true;
    }

    private static void registerCopperGolemScreen() {
        try {
            Class<?> constructorType = Class.forName("net.minecraft.client.gui.screens.MenuScreens$ScreenConstructor");
            Object factory = Proxy.newProxyInstance(
                    constructorType.getClassLoader(),
                    new Class<?>[]{constructorType},
                    (proxy, method, arguments) -> switch (method.getName()) {
                        case "create" -> new CopperGolemMenuScreen(
                                (CopperGolemMenu) arguments[0],
                                (Inventory) arguments[1],
                                (Component) arguments[2]);
                        case "fromPacket" -> null;
                        case "toString" -> "TotemAutomata copper golem screen factory";
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "equals" -> proxy == arguments[0];
                        default -> throw new UnsupportedOperationException(
                                "Unsupported MenuScreens factory method: " + method);
                    });
            MenuScreensAccessor.totemAutomata$getScreens().put(CopperGolemMenuRegistration.TYPE, factory);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to register Automata Copper Golem screen", exception);
        }
    }
}
