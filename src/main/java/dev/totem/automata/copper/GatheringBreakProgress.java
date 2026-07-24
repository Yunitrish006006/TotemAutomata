package dev.totem.automata.copper;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.state.BlockState;

/** Persisted visible break timing/progress for gathering. */
public final class GatheringBreakProgress {
    private static final String TICKS="deadrecall_gathering_break_ticks", REQUIRED="deadrecall_gathering_break_required_ticks", STATE="deadrecall_gathering_break_state";
    private GatheringBreakProgress() { }
    public static Step advance(CompoundTag tag, ServerLevel level, net.minecraft.core.BlockPos pos, BlockState state, ItemStack tool) {
        int required = requiredTicks(level, pos, state, tool); String key = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        int progress = tag.getIntOr(TICKS, 0); if (tag.getIntOr(REQUIRED, required) != required || !key.equals(tag.getStringOr(STATE, ""))) progress = 0;
        progress++; tag.putInt(TICKS, progress); tag.putInt(REQUIRED, required); tag.putString(STATE, key);
        return new Step(progress, required, Math.min(9, (int) ((progress / (double) required) * 10D)), progress >= required);
    }
    public static void clear(CompoundTag tag) { tag.remove(TICKS); tag.remove(REQUIRED); tag.remove(STATE); }
    public static int requiredTicks(ServerLevel level, net.minecraft.core.BlockPos pos, BlockState state, ItemStack tool) {
        float hardness = state.getDestroySpeed(level, pos); if (hardness <= 0) return 1;
        float speed = tool.isEmpty() ? 1F : Math.max(1F, tool.getDestroySpeed(state));
        if (!tool.isEmpty() && speed > 1F) { final double[] bonus={0}; EnchantmentHelper.forEachModifier(tool, EquipmentSlot.MAINHAND, (attribute, modifier) -> { if (attribute.equals(Attributes.MINING_EFFICIENCY) && modifier.operation()== AttributeModifier.Operation.ADD_VALUE) bonus[0]+=modifier.amount(); }); speed += (float) bonus[0]; }
        speed = Math.max(.1F, speed * .5F); boolean correct = !state.requiresCorrectToolForDrops() || tool.isCorrectToolForDrops(state);
        return Math.max(8, Math.max(1, (int) Math.ceil(hardness * (correct ? 30F : 100F) / speed)));
    }
    public record Step(int progressTicks, int requiredTicks, int crackStage, boolean complete) { }
}
