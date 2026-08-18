package dev.totem.automata.excavation;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

import java.util.Set;

/**
 * Isolated optional-module boundary for Totem Excavation hammers.
 *
 * <p>The adapter deliberately has no direct Java reference to Excavation. Its
 * fixed item-ID contract is evaluated only after Fabric reports the optional
 * module is present, so a standalone Automata runtime cannot resolve an
 * Excavation class. Copper Golem gathering intentionally owns its own target
 * and break transaction, never a player's hammer session.</p>
 */
public final class TotemExcavationHammerAdapter {
    public static final String MOD_ID = "totem-excavation";
    private static final Set<String> HAMMER_IDS = Set.of(
            "totem:excavation/wooden_hammer",
            "totem:excavation/stone_hammer",
            "totem:excavation/copper_hammer",
            "totem:excavation/iron_hammer",
            "totem:excavation/golden_hammer",
            "totem:excavation/diamond_hammer",
            "totem:excavation/netherite_hammer"
    );

    private TotemExcavationHammerAdapter() {
    }

    public static boolean isAvailable() {
        return FabricLoader.getInstance().isModLoaded(MOD_ID);
    }

    public static boolean isSupported(ItemStack stack) {
        return !stack.isEmpty()
                && isAvailable()
                && HAMMER_IDS.contains(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
    }
}
