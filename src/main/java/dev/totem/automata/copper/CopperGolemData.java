package dev.totem.automata.copper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Persisted Copper Golem schema shared by sorting, gathering and restart recovery. */
public final class CopperGolemData {
    public static final int DATA_VERSION = 2;
    public static final String TAG_DATA_VERSION = "deadrecall_data_version";
    public static final String TAG_REVISION = "deadrecall_revision";
    public static final String TAG_MODE = "deadrecall_mode";
    public static final String TAG_TRANSPORT_ENABLED = "deadrecall_transport_enabled";
    public static final String TAG_ACTIVITY = "deadrecall_activity";
    public static final String TAG_FUEL_STACK = "deadrecall_fuel_stack";
    public static final String TAG_FUEL_TICKS = "deadrecall_fuel_ticks";
    public static final String TAG_BOUND_CONTAINERS = "deadrecall_bound_containers";
    public static final String TAG_BOUND_CONTAINER_DIM = "deadrecall_bound_container_dim";
    public static final String TAG_BOUND_CONTAINER_X = "deadrecall_bound_container_x";
    public static final String TAG_BOUND_CONTAINER_Y = "deadrecall_bound_container_y";
    public static final String TAG_BOUND_CONTAINER_Z = "deadrecall_bound_container_z";
    public static final String TAG_BINDING_DIM = "dimension";
    public static final String TAG_BINDING_X = "x";
    public static final String TAG_BINDING_Y = "y";
    public static final String TAG_BINDING_Z = "z";

    private CopperGolemData() {
    }

    /** Applies the legacy-compatible schema defaults and one-to-many binding migration. */
    public static boolean migrate(CompoundTag tag) {
        boolean changed = false;
        if (tag.getIntOr(TAG_DATA_VERSION, 0) < DATA_VERSION) {
            tag.putInt(TAG_DATA_VERSION, DATA_VERSION);
            changed = true;
        }
        if (!tag.contains(TAG_MODE)) {
            tag.putString(TAG_MODE, CopperGolemMode.SORTING.id());
            changed = true;
        }
        if (!tag.contains(TAG_REVISION)) {
            tag.putInt(TAG_REVISION, 0);
            changed = true;
        }
        return migrateLegacySortingBindings(tag) || changed;
    }

    public static void bumpRevision(CompoundTag tag) {
        tag.putInt(TAG_REVISION, tag.getIntOr(TAG_REVISION, 0) + 1);
        tag.putInt(TAG_DATA_VERSION, DATA_VERSION);
    }

    public static CopperGolemMode mode(CompoundTag tag) {
        return CopperGolemMode.fromId(tag.getStringOr(TAG_MODE, CopperGolemMode.SORTING.id()));
    }

    public static CopperGolemActivity activity(CompoundTag tag) {
        return CopperGolemActivity.fromId(tag.getStringOr(TAG_ACTIVITY, ""));
    }

    public static List<CopperGolemBinding> readBindings(CompoundTag tag) {
        List<CopperGolemBinding> bindings = new ArrayList<>(readBindingList(tag, TAG_BOUND_CONTAINERS));
        readBinding(tag, TAG_BOUND_CONTAINER_DIM, TAG_BOUND_CONTAINER_X, TAG_BOUND_CONTAINER_Y, TAG_BOUND_CONTAINER_Z)
                .filter(binding -> !bindings.contains(binding))
                .ifPresent(bindings::add);
        return List.copyOf(bindings);
    }

    public static void writeBindings(CompoundTag tag, List<CopperGolemBinding> bindings) {
        ListTag list = new ListTag();
        for (CopperGolemBinding binding : bindings) {
            CompoundTag bindingTag = new CompoundTag();
            writeBinding(bindingTag, binding, TAG_BINDING_DIM, TAG_BINDING_X, TAG_BINDING_Y, TAG_BINDING_Z);
            list.add(bindingTag);
        }
        tag.put(TAG_BOUND_CONTAINERS, list);
        tag.remove(TAG_BOUND_CONTAINER_DIM);
        tag.remove(TAG_BOUND_CONTAINER_X);
        tag.remove(TAG_BOUND_CONTAINER_Y);
        tag.remove(TAG_BOUND_CONTAINER_Z);
    }

    public static Optional<CopperGolemBinding> readBinding(
            CompoundTag tag, String dimensionKey, String xKey, String yKey, String zKey) {
        if (!tag.contains(dimensionKey) || !tag.contains(xKey) || !tag.contains(yKey) || !tag.contains(zKey)) {
            return Optional.empty();
        }
        Identifier dimensionId = Identifier.tryParse(tag.getStringOr(dimensionKey, ""));
        if (dimensionId == null) return Optional.empty();
        return Optional.of(new CopperGolemBinding(
                ResourceKey.create(Registries.DIMENSION, dimensionId),
                new BlockPos(tag.getIntOr(xKey, 0), tag.getIntOr(yKey, 0), tag.getIntOr(zKey, 0))));
    }

    public static void writeBinding(
            CompoundTag tag, CopperGolemBinding binding, String dimensionKey, String xKey, String yKey, String zKey) {
        tag.putString(dimensionKey, binding.dimension().identifier().toString());
        tag.putInt(xKey, binding.containerPos().getX());
        tag.putInt(yKey, binding.containerPos().getY());
        tag.putInt(zKey, binding.containerPos().getZ());
    }

    public static ItemStack readItemStack(CompoundTag tag, String key) {
        return tag.read(key, ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY).copy();
    }

    public static void writeItemStack(CompoundTag tag, String key, ItemStack stack) {
        if (stack.isEmpty()) {
            tag.remove(key);
        } else {
            tag.store(key, ItemStack.OPTIONAL_CODEC, stack.copy());
        }
    }

    public static CompoundTag readEntityTag(Entity entity) {
        CustomData customData = entity.get(DataComponents.CUSTOM_DATA);
        return customData == null ? new CompoundTag() : customData.copyTag();
    }

    public static void writeEntityTag(Entity entity, CompoundTag tag) {
        entity.setComponent(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private static boolean migrateLegacySortingBindings(CompoundTag tag) {
        boolean hasLegacyBinding = tag.contains(TAG_BOUND_CONTAINER_DIM)
                || tag.contains(TAG_BOUND_CONTAINER_X)
                || tag.contains(TAG_BOUND_CONTAINER_Y)
                || tag.contains(TAG_BOUND_CONTAINER_Z);
        if (!hasLegacyBinding) return false;
        writeBindings(tag, readBindings(tag));
        return true;
    }

    private static List<CopperGolemBinding> readBindingList(CompoundTag tag, String listKey) {
        List<CopperGolemBinding> bindings = new ArrayList<>();
        tag.getList(listKey).ifPresent(list -> {
            for (CompoundTag bindingTag : list.compoundStream().toList()) {
                readBinding(bindingTag, TAG_BINDING_DIM, TAG_BINDING_X, TAG_BINDING_Y, TAG_BINDING_Z)
                        .filter(binding -> !bindings.contains(binding))
                        .ifPresent(bindings::add);
            }
        });
        return bindings;
    }
}
