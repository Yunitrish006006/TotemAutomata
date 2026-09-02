package dev.totem.automata.registry;

import dev.totem.automata.advancement.AutomataCriteria;
import dev.totem.automata.copper.CopperWrenchSelection;
import dev.totem.automata.item.CopperWrenchItem;
import dev.totem.automata.menu.CopperGolemMenuRegistration;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Automata-owned canonical registrations. DeadRecall owns legacy aliases.
 */
public final class AutomataRegistries {
    private static final ResourceKey<CreativeModeTab> TAB_KEY = ResourceKey.create(
            Registries.CREATIVE_MODE_TAB,
            Identifier.fromNamespaceAndPath("totem-automata", "main")
    );

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

    /** Adds the canonical Wrench to Automata's module-owned Creative tab. */
    public static synchronized void registerCreativeTabEntry() {
        if (creativeTabRegistered) {
            return;
        }
        registerCreativeTab();
        CreativeModeTabEvents.modifyOutputEvent(TAB_KEY)
                .register(output -> output.accept(COPPER_WRENCH));
        creativeTabRegistered = true;
    }

    private static void registerCreativeTab() {
        if (BuiltInRegistries.CREATIVE_MODE_TAB.getOptional(TAB_KEY).isPresent()) {
            return;
        }
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, TAB_KEY,
                FabricCreativeModeTab.builder()
                        .title(Component.translatable("itemGroup.totem_automata.main"))
                        .icon(() -> new ItemStack(COPPER_WRENCH))
                        .build());
    }

    private static Item registerItem(Identifier id, java.util.function.Function<Item.Properties, Item> factory) {
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id);
        if (BuiltInRegistries.ITEM.containsKey(id)) {
            return BuiltInRegistries.ITEM.getValue(id);
        }
        return Registry.register(
                BuiltInRegistries.ITEM,
                id,
                factory.apply(new Item.Properties().setId(itemKey))
        );
    }
}
