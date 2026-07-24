package dev.totem.automata.copper;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

/** Atomic persisted state prepared immediately before a successful gathering break. */
public final class GatheringBreakTransaction {
    private static final String STORAGE = "deadrecall_gathering_storage_stack", TOOL = "deadrecall_gathering_tool_stack";
    private GatheringBreakTransaction() { }
    public static Optional<Result> prepare(CompoundTag current, ServerLevel level, ItemStack storage, ItemStack tool, List<ItemStack> drops) {
        CompoundTag tag = current.copy();
        if (!CopperGolemFuelService.consumeForTransport(tag, level)) return Optional.empty();
        CopperGolemData.writeItemStack(tag, STORAGE, GatheringStorage.addDrops(storage, drops));
        GatheringToolDamage.Result damage = GatheringToolDamage.apply(level, tool);
        CopperGolemData.writeItemStack(tag, TOOL, damage.stack().isEmpty() ? ItemStack.EMPTY : damage.stack().copyWithCount(1));
        GatheringRuntimeState.clearTarget(tag);
        return Optional.of(new Result(tag, damage.broken()));
    }
    public record Result(CompoundTag tag, boolean toolBroken) { }
}
