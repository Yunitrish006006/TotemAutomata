package dev.totem.automata.copper;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.behavior.TransportItemsBetweenContainers.TransportItemTarget;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Optional;
import java.util.OptionalInt;

/** Sorting controller separated from the Wrench/menu authority through {@link SortingOperations}. */
public final class SortingModeController {
    private static final int BLOCKED_JUMP_INTERVAL_TICKS = 10;
    private SortingModeController() { }

    public static boolean isBindingInLevel(CopperGolem golem, Level level, SortingOperations ops) {
        return ops.bindings(golem).stream().anyMatch(binding -> binding.dimension().equals(level.dimension()));
    }
    public static boolean isBoundContainer(CopperGolem golem, Level level, BlockPos pos, SortingOperations ops) {
        return ops.bindings(golem).stream().anyMatch(binding -> binding.dimension().equals(level.dimension()) && binding.containerPos().equals(pos));
    }
    public static Optional<TransportItemTarget> nextDestination(CopperGolem golem, ServerLevel level, ItemStack carried, SortingOperations ops) {
        if (carried.isEmpty()) return Optional.empty();
        for (CopperGolemBinding binding : ops.bindings(golem)) {
            if (!binding.dimension().equals(level.dimension()) || ops.triedDestinations(golem).contains(binding)
                    || ops.rememberedSource(golem).filter(source -> source.dimension().equals(binding.dimension()) && source.containerPos().equals(binding.containerPos())).isPresent()) continue;
            TransportItemTarget target = ops.target(level, binding.containerPos());
            ops.rememberTriedDestination(golem, binding);
            if (target != null && ops.canAccept(golem, level, binding, target.container(), carried)) return Optional.of(target);
        }
        return Optional.empty();
    }
    public static ItemStack pickUp(CopperGolem golem, ServerLevel level, Container source, BlockPos sourcePos, SortingOperations ops) {
        if (!ops.hasFuel(golem, level)) return ItemStack.EMPTY;
        OptionalInt sortableSlot = ops.sortableSourceSlot(golem, level, source, sourcePos);
        if (sortableSlot.isEmpty()) {
            if (!source.isEmpty() && !ops.awaitingSortingDecision(golem, level, source, sourcePos)) {
                ops.markBlocked(golem, level, sourcePos, source);
            }
            return ItemStack.EMPTY;
        }
        int slot = sortableSlot.getAsInt();
        ItemStack stack = source.getItem(slot);
        if (stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack picked = source.removeItem(slot, Math.min(stack.getCount(), ops.maxTransportStackSize()));
        if (!picked.isEmpty()) { ops.rememberSource(golem, level, sourcePos, slot); ops.consumeFuel(golem, level); }
        return picked;
    }
    public static boolean returnCarried(CopperGolem golem, ServerLevel level, SortingOperations ops) {
        ItemStack carried = golem.getMainHandItem();
        if (carried.isEmpty()) { ops.clearRememberedSource(golem); return true; }
        Optional<SortingOperations.Source> source = ops.rememberedSource(golem);
        if (source.isEmpty() || !source.get().dimension().equals(level.dimension())) return false;
        TransportItemTarget target = ops.target(level, source.get().containerPos()); if (target == null) return false;
        ItemStack remaining = ops.returnToSource(target.container(), carried, source.get().slot()); target.container().setChanged();
        golem.setItemInHand(InteractionHand.MAIN_HAND, remaining);
        if (remaining.isEmpty()) { ops.clearRememberedSource(golem); return true; }
        return false;
    }
    public static Optional<ItemStack> deposit(CopperGolem golem, ServerLevel level, BlockPos targetPos, Container container, SortingOperations ops) {
        ItemStack carried = golem.getMainHandItem(); if (carried.isEmpty()) return Optional.empty();
        Optional<CopperGolemBinding> binding = ops.bindings(golem).stream().filter(value -> value.dimension().equals(level.dimension()) && value.containerPos().equals(targetPos)).findFirst();
        if (binding.isEmpty() || !ops.acceptsByCachedDecision(golem, binding.get(), carried)) return Optional.of(carried);
        ItemStack remaining = SortingDestinationService.insert(container, carried);
        if (remaining.getCount() < carried.getCount()) container.setChanged();
        return Optional.of(remaining);
    }
    public static void tickBlocked(CopperGolem golem, ServerLevel level, SortingOperations ops) {
        if (golem.tickCount % BLOCKED_JUMP_INTERVAL_TICKS == 0 && ops.shouldClearBlocked(golem, level)) { ops.clearBlocked(golem); return; }
        golem.getNavigation().stop(); golem.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET); golem.setDeltaMovement(0, golem.getDeltaMovement().y, 0);
        if (golem.onGround() && golem.tickCount % BLOCKED_JUMP_INTERVAL_TICKS == 0) { golem.jumpFromGround(); golem.setJumping(true); }
    }
}
