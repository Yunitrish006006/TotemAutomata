package dev.totem.automata.copper;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/** Legacy enchantment-aware one-use tool damage applied after a gathered break. */
public final class GatheringToolDamage {
    private GatheringToolDamage() { }
    public static Result apply(ServerLevel level, ItemStack tool) {
        if (tool.isEmpty() || !tool.isDamageableItem()) return new Result(tool.copy(), false);
        ItemStack damaged = tool.copy();
        int amount = EnchantmentHelper.processDurabilityChange(level, damaged, 1);
        damaged.setDamageValue(damaged.getDamageValue() + Math.max(0, amount));
        return damaged.getDamageValue() >= damaged.getMaxDamage() ? new Result(ItemStack.EMPTY, true) : new Result(damaged, false);
    }
    public record Result(ItemStack stack, boolean broken) { }
}
