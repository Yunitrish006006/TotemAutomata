package dev.totem.automata.copper;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/** Stable registry-backed values supplied to the preserved gathering LLM prompt. */
public final class GatheringLlmPromptData {
    private GatheringLlmPromptData() {
    }

    public static List<String> blockTags(BlockState state) {
        return state.getBlock().builtInRegistryHolder().tags()
                .map(tag -> tag.location().toString())
                .sorted()
                .toList();
    }

    public static List<String> dropSummary(List<ItemStack> drops) {
        return drops.stream()
                .filter(stack -> !stack.isEmpty())
                .map(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()) + " x" + stack.getCount())
                .toList();
    }

    public static String toolSummary(ItemStack tool) {
        if (tool.isEmpty()) {
            return "none";
        }
        String itemId = BuiltInRegistries.ITEM.getKey(tool.getItem()).toString();
        if (!tool.isDamageableItem()) {
            return itemId;
        }
        return itemId + " durability " + Math.max(0, tool.getMaxDamage() - tool.getDamageValue())
                + "/" + tool.getMaxDamage();
    }
}
