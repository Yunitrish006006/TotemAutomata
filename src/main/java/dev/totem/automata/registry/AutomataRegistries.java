package dev.totem.automata.registry;

import dev.totem.automata.advancement.AutomataCriteria;
import dev.totem.automata.copper.CopperWrenchSelection;
import dev.totem.automata.item.CopperWrenchItem;
import dev.totem.automata.menu.CopperGolemMenuRegistration;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

/**
 * Automata-owned registrations that preserve the established {@code deadrecall}
 * namespace.  Nothing calls these methods during the additive phase: the
 * cutover bootstrap invokes them in DeadRecall's legacy registry order.
 */
public final class AutomataRegistries {
    private static final Identifier DEADRECALL_MAIN_TAB =
            Identifier.fromNamespaceAndPath("deadrecall", "main");

    public static final Item COPPER_WRENCH = registerItem(
            CopperWrenchSelection.ITEM_ID,
            properties -> new CopperWrenchItem(properties.stacksTo(1))
    );

    private static boolean creativeTabRegistered;

    private AutomataRegistries() {
    }

    /** Preserves the legacy criterion-before-menu ordering. */
    public static void registerCriteria() {
        AutomataCriteria.register();
    }

    public static void registerMenus() {
        CopperGolemMenuRegistration.register();
    }

    public static void registerItems() {
        // Class initialization owns the preserved item registration.
    }

    /**
     * Adds the external Wrench instance to DeadRecall's preserved creative
     * tab without a Java dependency on the compatibility bundle.
     */
    public static synchronized void registerCreativeTabEntry() {
        if (creativeTabRegistered) {
            return;
        }
        CreativeModeTabEvents.modifyOutputEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB, DEADRECALL_MAIN_TAB))
                .register(output -> output.accept(COPPER_WRENCH));
        creativeTabRegistered = true;
    }

    private static Item registerItem(Identifier id, java.util.function.Function<Item.Properties, Item> factory) {
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id);
        return Registry.register(BuiltInRegistries.ITEM, id, factory.apply(new Item.Properties().setId(itemKey)));
    }
}
