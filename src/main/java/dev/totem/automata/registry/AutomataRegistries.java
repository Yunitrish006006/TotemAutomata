package dev.totem.automata.registry;

import dev.totem.automata.advancement.AutomataCriteria;
import dev.totem.automata.copper.CopperWrenchSelection;
import dev.totem.automata.item.CopperWrenchItem;
import dev.totem.automata.menu.CopperGolemMenuRegistration;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.fabricmc.loader.api.FabricLoader;
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
 * Automata-owned registrations exposing a canonical item while preserving the
 * established {@code deadrecall} identifier for old saves.
 */
public final class AutomataRegistries {
    private static final Identifier DEADRECALL_MAIN_TAB =
            Identifier.fromNamespaceAndPath("deadrecall", "main");
    private static final ResourceKey<CreativeModeTab> DEADRECALL_MAIN_TAB_KEY =
            ResourceKey.create(Registries.CREATIVE_MODE_TAB, DEADRECALL_MAIN_TAB);

    public static final Item COPPER_WRENCH = registerItem(
            CopperWrenchSelection.ITEM_ID,
            properties -> new CopperWrenchItem(properties.stacksTo(1))
    );
    public static final Item LEGACY_COPPER_WRENCH = registerItem(
            CopperWrenchSelection.LEGACY_ITEM_ID,
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
        registerStandaloneCreativeTab();
        CreativeModeTabEvents.modifyOutputEvent(DEADRECALL_MAIN_TAB_KEY)
                .register(output -> output.accept(COPPER_WRENCH));
        creativeTabRegistered = true;
    }

    /**
     * The compatibility bundle owns this tab when it is installed.  A
     * standalone feature still needs the same legacy tab so its only item is
     * discoverable in Creative mode.  The registry lookup lets independently
     * loaded feature modules share one tab without registering it twice.
     */
    private static void registerStandaloneCreativeTab() {
        if (FabricLoader.getInstance().isModLoaded("deadrecall")
                || BuiltInRegistries.CREATIVE_MODE_TAB.getOptional(DEADRECALL_MAIN_TAB_KEY).isPresent()) {
            return;
        }
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, DEADRECALL_MAIN_TAB_KEY,
                FabricCreativeModeTab.builder()
                        .title(Component.translatable("itemGroup.deadrecall.main"))
                        .icon(() -> new ItemStack(COPPER_WRENCH))
                        .build());
    }

    private static Item registerItem(Identifier id, java.util.function.Function<Item.Properties, Item> factory) {
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id);
        return BuiltInRegistries.ITEM.getOptional(itemKey).orElseGet(() ->
                Registry.register(BuiltInRegistries.ITEM, id,
                        factory.apply(new Item.Properties().setId(itemKey))));
    }
}
