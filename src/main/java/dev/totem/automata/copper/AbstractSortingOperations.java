package dev.totem.automata.copper;

import dev.totem.automata.containersafety.ContainerSafetyBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

/** Data/fuel/source implementation shared by the future live sorting authority. */
public abstract class AbstractSortingOperations implements SortingOperations {
    private static final String SOURCE_SLOT = "deadrecall_source_slot";
    private static final String REMEMBERED_SOURCE_DIM = "totem_automata_transport_source_dim";
    private static final String REMEMBERED_SOURCE_X = "totem_automata_transport_source_x";
    private static final String REMEMBERED_SOURCE_Y = "totem_automata_transport_source_y";
    private static final String REMEMBERED_SOURCE_Z = "totem_automata_transport_source_z";
    private static final String TRIED_DESTINATIONS = "deadrecall_tried_destinations";
    private static final String BLOCKED = "deadrecall_sorting_blocked",
            BLOCKED_ACCESS = "totem_automata_sorting_access_blocked",
            BLOCKED_SOURCE_DIM = "deadrecall_blocked_source_container_dim",
            BLOCKED_SOURCE_X = "deadrecall_blocked_source_container_x", BLOCKED_SOURCE_Y = "deadrecall_blocked_source_container_y",
            BLOCKED_SOURCE_Z = "deadrecall_blocked_source_container_z", BLOCKED_SOURCE_HASH = "deadrecall_blocked_source_hash",
            BLOCKED_BINDINGS_HASH = "deadrecall_blocked_bindings_hash", BLOCKED_TARGETS_HASH = "deadrecall_blocked_targets_hash",
            BLOCKED_RETRY_DELAY = "totem_automata_sorting_blocked_retry_delay",
            BLOCKED_NEXT_RETRY = "totem_automata_sorting_blocked_next_retry_tick";

    @Override public List<CopperGolemBinding> bindings(CopperGolem golem) { return CopperGolemData.readBindings(tag(golem)); }
    @Override public boolean canAccept(CopperGolem golem, ServerLevel level, CopperGolemBinding binding, Container container, ItemStack carried) {
        return acceptsByCachedDecision(golem, binding, carried) && SortingDestinationService.canAccept(container, carried);
    }
    @Override public OptionalInt sortableSourceSlot(CopperGolem golem, ServerLevel level, Container source, BlockPos sourcePos) {
        RouteSnapshot snapshot = routeSnapshot(golem);
        for (int slot = 0; slot < source.getContainerSize(); slot++) {
            ItemStack stack = source.getItem(slot);
            if (stack.isEmpty()) continue;
            ItemStack candidate = stack.copyWithCount(Math.min(stack.getCount(), maxTransportStackSize()));
            for (CopperGolemBinding binding : snapshot.bindings()) {
                if (!binding.dimension().equals(level.dimension()) || binding.containerPos().equals(sourcePos)) continue;
                var target = target(level, binding.containerPos());
                if (target != null
                        && mayTransfer(golem, level, sourcePos, binding.containerPos())
                        && canAccept(golem, level, binding, target.container(), candidate, snapshot)) {
                    return OptionalInt.of(slot);
                }
            }
        }
        return OptionalInt.empty();
    }
    @Override public boolean hasAnySortableItem(CopperGolem golem, ServerLevel level, Container source, BlockPos sourcePos) {
        return sortableSourceSlot(golem, level, source, sourcePos).isPresent();
    }
    @Override public List<CopperGolemBinding> triedDestinations(CopperGolem golem) {
        CompoundTag tag = tag(golem);
        return readBindingList(tag, TRIED_DESTINATIONS);
    }
    @Override public Optional<Source> rememberedSource(CopperGolem golem) {
        return rememberedSource(tag(golem));
    }
    private static Optional<Source> rememberedSource(CompoundTag tag) {
        if (!tag.contains(SOURCE_SLOT)) return Optional.empty();
        Optional<CopperGolemBinding> remembered = CopperGolemData.readBinding(
                tag,
                REMEMBERED_SOURCE_DIM,
                REMEMBERED_SOURCE_X,
                REMEMBERED_SOURCE_Y,
                REMEMBERED_SOURCE_Z
        );
        // 0.1.12 recovery compatibility: older in-flight transfers stored only
        // the slot and reused the configured source binding. Read that once as
        // a fallback, but never clear or overwrite the configured source now.
        if (remembered.isEmpty()) {
            remembered = SortingBindingService.readSourceContainer(tag);
        }
        return remembered.map(binding -> new Source(
                binding.dimension(),
                binding.containerPos(),
                tag.getIntOr(SOURCE_SLOT, 0)
        ));
    }
    @Override public net.minecraft.world.entity.ai.behavior.TransportItemsBetweenContainers.TransportItemTarget target(ServerLevel level, BlockPos pos) {
        return net.minecraft.world.entity.ai.behavior.TransportItemsBetweenContainers.TransportItemTarget.tryCreatePossibleTarget(pos, level);
    }
    @Override public void rememberTriedDestination(CopperGolem golem, CopperGolemBinding binding) {
        rememberTriedDestinations(golem, List.of(binding));
    }
    @Override public void rememberTriedDestinations(CopperGolem golem, List<CopperGolemBinding> attempted) {
        if (attempted.isEmpty()) return;
        CompoundTag tag = tag(golem);
        List<CopperGolemBinding> tried = new ArrayList<>(readBindingList(tag, TRIED_DESTINATIONS));
        boolean changed = false;
        for (CopperGolemBinding binding : attempted) {
            if (!tried.contains(binding)) {
                tried.add(binding);
                changed = true;
            }
        }
        if (!changed) return;
        writeBindingList(tag, TRIED_DESTINATIONS, tried);
        write(golem, tag);
    }

    @Override public RouteSnapshot routeSnapshot(CopperGolem golem) {
        CompoundTag snapshot = tag(golem);
        return new RouteSnapshot(
                CopperGolemData.readBindings(snapshot),
                readBindingList(snapshot, TRIED_DESTINATIONS),
                rememberedSource(snapshot),
                snapshot
        );
    }
    @Override public boolean hasFuel(CopperGolem golem, ServerLevel level) { return CopperGolemFuelService.hasFuelAvailable(tag(golem), level); }
    @Override public int maxTransportStackSize() { return 16; }
    @Override public void rememberSource(CopperGolem golem, ServerLevel level, BlockPos sourcePos, int slot) {
        CompoundTag tag = tag(golem);
        CopperGolemData.writeBinding(
                tag,
                new CopperGolemBinding(level.dimension(), sourcePos.immutable()),
                REMEMBERED_SOURCE_DIM,
                REMEMBERED_SOURCE_X,
                REMEMBERED_SOURCE_Y,
                REMEMBERED_SOURCE_Z
        );
        tag.putInt(SOURCE_SLOT, slot); tag.remove(TRIED_DESTINATIONS); write(golem, tag);
    }
    @Override public void consumeFuel(CopperGolem golem, ServerLevel level) {
        CompoundTag tag = tag(golem); if (CopperGolemFuelService.consumeForTransport(tag, level)) write(golem, tag);
    }
    @Override public void markBlocked(CopperGolem golem, ServerLevel level, BlockPos sourcePos, Container source) {
        CompoundTag tag = tag(golem); tag.putBoolean(BLOCKED, true); tag.remove(BLOCKED_ACCESS);
        List<CopperGolemBinding> bindings = CopperGolemData.readBindings(tag);
        CopperGolemData.writeBinding(tag, new CopperGolemBinding(level.dimension(), sourcePos.immutable()), BLOCKED_SOURCE_DIM, BLOCKED_SOURCE_X, BLOCKED_SOURCE_Y, BLOCKED_SOURCE_Z);
        tag.putInt(BLOCKED_SOURCE_HASH, hash(source)); tag.putInt(BLOCKED_BINDINGS_HASH, hashBindings(bindings)); tag.putInt(BLOCKED_TARGETS_HASH, hashTargets(golem, level, bindings));
        initializeBlockedRetry(tag, golem.tickCount);
        tag.remove(TRIED_DESTINATIONS); clearRememberedSource(tag); write(golem, tag);
    }
    @Override public void markAccessBlocked(CopperGolem golem, ServerLevel level, BlockPos sourcePos) {
        CompoundTag tag = tag(golem);
        tag.putBoolean(BLOCKED, true);
        tag.putBoolean(BLOCKED_ACCESS, true);
        CopperGolemData.writeBinding(tag, new CopperGolemBinding(level.dimension(), sourcePos.immutable()),
                BLOCKED_SOURCE_DIM, BLOCKED_SOURCE_X, BLOCKED_SOURCE_Y, BLOCKED_SOURCE_Z);
        tag.remove(BLOCKED_SOURCE_HASH);
        tag.remove(BLOCKED_BINDINGS_HASH);
        tag.remove(BLOCKED_TARGETS_HASH);
        initializeBlockedRetry(tag, golem.tickCount);
        tag.remove(TRIED_DESTINATIONS);
        clearRememberedSource(tag);
        write(golem, tag);
    }
    @Override public boolean shouldClearBlocked(CopperGolem golem, ServerLevel level) {
        CompoundTag tag = tag(golem);
        // Permission-denied sources deliberately store no content hashes. Clear
        // the transient blocked state on the retry cadence so the next pickup
        // attempt re-checks Locksmith without inspecting the container first.
        if (tag.getBooleanOr(BLOCKED_ACCESS, false)) return true;
        Optional<CopperGolemBinding> source = CopperGolemData.readBinding(tag, BLOCKED_SOURCE_DIM, BLOCKED_SOURCE_X, BLOCKED_SOURCE_Y, BLOCKED_SOURCE_Z);
        if (source.isEmpty() || !source.get().dimension().equals(level.dimension())) return true;
        var target = target(level, source.get().containerPos()); if (target == null) return true;
        List<CopperGolemBinding> bindings = CopperGolemData.readBindings(tag);
        return tag.getIntOr(BLOCKED_SOURCE_HASH, 0) != hash(target.container())
                || tag.getIntOr(BLOCKED_BINDINGS_HASH, 0) != hashBindings(bindings)
                || tag.getIntOr(BLOCKED_TARGETS_HASH, 0) != hashTargets(golem, level, bindings);
    }
    @Override public boolean blockedRetryDue(CopperGolem golem) {
        CompoundTag tag = tag(golem);
        return SortingBlockedBackoff.due(golem.tickCount, tag.getLongOr(BLOCKED_NEXT_RETRY, 0));
    }
    @Override public void advanceBlockedRetry(CopperGolem golem) {
        CompoundTag tag = tag(golem);
        int delay = SortingBlockedBackoff.nextDelay(tag.getIntOr(BLOCKED_RETRY_DELAY, 0));
        tag.putInt(BLOCKED_RETRY_DELAY, delay);
        tag.putLong(BLOCKED_NEXT_RETRY, (long) golem.tickCount + delay);
        write(golem, tag);
    }
    @Override public void clearBlocked(CopperGolem golem) {
        CompoundTag tag = tag(golem); boolean changed = false;
        for (String key : List.of(BLOCKED, BLOCKED_ACCESS, BLOCKED_SOURCE_DIM, BLOCKED_SOURCE_X, BLOCKED_SOURCE_Y, BLOCKED_SOURCE_Z, BLOCKED_SOURCE_HASH, BLOCKED_BINDINGS_HASH, BLOCKED_TARGETS_HASH, BLOCKED_RETRY_DELAY, BLOCKED_NEXT_RETRY)) {
            if (tag.contains(key)) { tag.remove(key); changed = true; }
        }
        if (clearRememberedSource(tag)) changed = true;
        if (changed) write(golem, tag);
    }
    @Override public ItemStack returnToSource(Container source, ItemStack carried, int sourceSlot) {
        if (carried.isEmpty() || source.getContainerSize() == 0) return carried;
        ItemStack remaining = carried.copy(); int first = Math.max(0, Math.min(sourceSlot, source.getContainerSize() - 1));
        merge(source, first, remaining); place(source, first, remaining);
        for (int slot = source.getContainerSize() - 1; slot > first && !remaining.isEmpty(); slot--) merge(source, slot, remaining);
        for (int slot = source.getContainerSize() - 1; slot > first && !remaining.isEmpty(); slot--) place(source, slot, remaining);
        return remaining;
    }
    @Override public void clearRememberedSource(CopperGolem golem) {
        CompoundTag tag = tag(golem);
        if (clearRememberedSource(tag)) write(golem, tag);
    }
    private static boolean clearRememberedSource(CompoundTag tag) {
        boolean changed = false;
        for (String key : List.of(
                REMEMBERED_SOURCE_DIM,
                REMEMBERED_SOURCE_X,
                REMEMBERED_SOURCE_Y,
                REMEMBERED_SOURCE_Z,
                SOURCE_SLOT,
                TRIED_DESTINATIONS
        )) {
            if (tag.contains(key)) {
                tag.remove(key);
                changed = true;
            }
        }
        return changed;
    }

    private static CompoundTag tag(CopperGolem golem) { return CopperGolemData.readEntityTag(golem); }
    private static void write(CopperGolem golem, CompoundTag tag) { CopperGolemData.writeEntityTag(golem, tag); }
    private static List<CopperGolemBinding> readBindingList(CompoundTag tag, String key) {
        List<CopperGolemBinding> result = new ArrayList<>();
        tag.getList(key).ifPresent(list -> list.compoundStream().map(value -> CopperGolemData.readBinding(value,
                CopperGolemData.TAG_BINDING_DIM, CopperGolemData.TAG_BINDING_X, CopperGolemData.TAG_BINDING_Y, CopperGolemData.TAG_BINDING_Z))
                .flatMap(Optional::stream).filter(value -> !result.contains(value)).forEach(result::add));
        return List.copyOf(result);
    }
    private static void writeBindingList(CompoundTag tag, String key, List<CopperGolemBinding> bindings) {
        net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
        for (CopperGolemBinding binding : bindings) { CompoundTag value = new CompoundTag(); CopperGolemData.writeBinding(value, binding,
                CopperGolemData.TAG_BINDING_DIM, CopperGolemData.TAG_BINDING_X, CopperGolemData.TAG_BINDING_Y, CopperGolemData.TAG_BINDING_Z); list.add(value); }
        tag.put(key, list);
    }
    private static void merge(Container container, int slot, ItemStack remaining) {
        if (!ContainerSafetyBridge.mayInsertIntoContainer(container, remaining)) return;
        ItemStack stack = container.getItem(slot);
        if (stack.isEmpty() || !ItemStack.isSameItemSameComponents(stack, remaining) || !container.canPlaceItem(slot, remaining)) return;
        int move = Math.min(remaining.getCount(), Math.min(stack.getMaxStackSize(), container.getMaxStackSize(remaining)) - stack.getCount());
        if (move > 0) { stack.grow(move); remaining.shrink(move); container.setItem(slot, stack); }
    }
    private static void place(Container container, int slot, ItemStack remaining) {
        if (!ContainerSafetyBridge.mayInsertIntoContainer(container, remaining)) return;
        if (remaining.isEmpty() || !container.getItem(slot).isEmpty() || !container.canPlaceItem(slot, remaining)) return;
        int move = Math.min(remaining.getCount(), Math.min(remaining.getMaxStackSize(), container.getMaxStackSize(remaining)));
        container.setItem(slot, remaining.copyWithCount(move)); remaining.shrink(move);
    }
    private static int hash(Container container) {
        int result = 1;
        for (int slot = 0; slot < container.getContainerSize(); slot++) { ItemStack stack = container.getItem(slot); result = 31 * result + slot;
            result = 31 * result + (stack.isEmpty() ? 0 : ItemStack.hashItemAndComponents(stack)); result = 31 * result + (stack.isEmpty() ? 0 : stack.getCount()); }
        return result;
    }
    private static int hashBindings(List<CopperGolemBinding> bindings) {
        int result = 1; for (CopperGolemBinding binding : bindings) { result = 31 * result + binding.dimension().identifier().hashCode(); result = 31 * result + binding.containerPos().hashCode(); } return result;
    }
    private int hashTargets(CopperGolem golem, ServerLevel level, List<CopperGolemBinding> bindings) {
        int result = 1;
        for (CopperGolemBinding binding : bindings) { result = 31 * result + binding.dimension().identifier().hashCode(); result = 31 * result + binding.containerPos().hashCode();
            if (binding.dimension().equals(level.dimension())) { var target = target(level, binding.containerPos()); result = 31 * result + (target == null ? 0 : hash(target.container())); } }
        return result;
    }
    private static void initializeBlockedRetry(CompoundTag tag, int gameTick) {
        tag.putInt(BLOCKED_RETRY_DELAY, SortingBlockedBackoff.INITIAL_DELAY_TICKS);
        tag.putLong(BLOCKED_NEXT_RETRY, (long) gameTick + SortingBlockedBackoff.INITIAL_DELAY_TICKS);
    }
}
