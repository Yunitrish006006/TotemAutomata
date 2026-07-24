package dev.totem.automata.copper;

import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.UUID;

/** Stable legacy Wrench item and ItemStack selection contract. */
public final class CopperWrenchSelection {
    public static final Identifier ITEM_ID = Identifier.fromNamespaceAndPath("deadrecall", "copper_wrench");
    public static final String SELECTED_GOLEM_KEY = "deadrecall_selected_golem";

    private CopperWrenchSelection() { }

    public static boolean isCopperWrench(ItemStack stack) {
        return !stack.isEmpty() && ITEM_ID.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    public static UUID selectedGolem(ItemStack stack) {
        if (!isCopperWrench(stack)) return null;
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.read(SELECTED_GOLEM_KEY, UUIDUtil.CODEC).orElse(null);
    }

    public static boolean select(ItemStack stack, UUID golemId) {
        if (!isCopperWrench(stack) || golemId == null) return false;
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.store(SELECTED_GOLEM_KEY, UUIDUtil.CODEC, golemId);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return true;
    }

    public static boolean clear(ItemStack stack) {
        if (!isCopperWrench(stack) || selectedGolem(stack) == null) return false;
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.remove(SELECTED_GOLEM_KEY);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return true;
    }
}
