package dev.totem.automata.containersafety;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Method;

/**
 * Optional Remnant adapter for sortable tiered backpacks.
 *
 * <p>Automata must not link against Remnant: a standalone installation simply
 * treats every stack as a normal container item. When Remnant is present, the
 * adapter recognizes only tiered backpacks (not death backpacks), matching the
 * legacy Copper Golem sorting behavior.</p>
 */
public final class RemnantBackpackBridge {
    private static volatile Access access;
    private static volatile boolean resolved;

    private RemnantBackpackBridge() { }

    public static boolean isBackpack(ItemStack stack) {
        Access adapter = access();
        return adapter != null && adapter.isBackpack(stack);
    }

    public static boolean isSortableTieredBackpack(ItemStack stack) {
        Access adapter = access();
        return adapter != null && adapter.isTiered(stack);
    }

    public static int tieredBackpackSlots(ItemStack stack) {
        Access adapter = access();
        return adapter == null ? 0 : adapter.slots(stack);
    }

    private static Access access() {
        if (resolved) return access;
        synchronized (RemnantBackpackBridge.class) {
            if (!resolved) { access = load(); resolved = true; }
            return access;
        }
    }

    private static Access load() {
        if (!FabricLoader.getInstance().isModLoaded("totem-remnant")) return null;
        try {
            ClassLoader loader = RemnantBackpackBridge.class.getClassLoader();
            Class<?> helper = Class.forName("dev.totem.remnant.item.BackpackItemHelper", false, loader);
            Class<?> tieredItem = Class.forName("dev.totem.remnant.item.TieredBackpackItem", false, loader);
            Method isBackpack = helper.getMethod("isBackpackItem", ItemStack.class);
            Method tier = tieredItem.getMethod("tier");
            Class<?> tierType = tier.getReturnType();
            Method slots = tierType.getMethod("slots");
            return new Access(tieredItem, isBackpack, tier, slots);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private record Access(Class<?> tieredItem, Method isBackpack, Method tier, Method slots) {
        boolean isBackpack(ItemStack stack) { return invokeBoolean(isBackpack, null, stack); }
        boolean isTiered(ItemStack stack) { return stack != null && !stack.isEmpty() && tieredItem.isInstance(stack.getItem()); }
        int slots(ItemStack stack) {
            if (!isTiered(stack)) return 0;
            try { return (int) slots.invoke(tier.invoke(stack.getItem())); }
            catch (ReflectiveOperationException ignored) { return 0; }
        }
        private static boolean invokeBoolean(Method method, Object target, Object... arguments) {
            try { return (boolean) method.invoke(target, arguments); }
            catch (ReflectiveOperationException ignored) { return false; }
        }
    }
}
