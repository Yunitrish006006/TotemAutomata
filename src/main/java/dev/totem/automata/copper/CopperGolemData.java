package dev.totem.automata.copper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Persisted Copper Golem schema shared by sorting, gathering and restart recovery. */
public final class CopperGolemData {
    public static final int DATA_VERSION = 3;
    public static final String TAG_DATA_VERSION = "totem_automata_data_version";
    public static final String TAG_REVISION = "totem_automata_revision";
    public static final String TAG_MODE = "totem_automata_mode";
    public static final String TAG_TRANSPORT_ENABLED = "totem_automata_transport_enabled";
    public static final String TAG_ACTIVITY = "totem_automata_activity";
    public static final String TAG_FUEL_STACK = "totem_automata_fuel_stack";
    public static final String TAG_FUEL_TICKS = "totem_automata_fuel_ticks";
    public static final String TAG_BOUND_CONTAINERS = "totem_automata_bound_containers";
    public static final String TAG_BOUND_CONTAINER_DIM = "totem_automata_bound_container_dim";
    public static final String TAG_BOUND_CONTAINER_X = "totem_automata_bound_container_x";
    public static final String TAG_BOUND_CONTAINER_Y = "totem_automata_bound_container_y";
    public static final String TAG_BOUND_CONTAINER_Z = "totem_automata_bound_container_z";
    public static final String TAG_BINDING_DIM = "dimension";
    public static final String TAG_BINDING_X = "x";
    public static final String TAG_BINDING_Y = "y";
    public static final String TAG_BINDING_Z = "z";

    /**
     * Enumerated one-way migration for every persisted Copper Golem key used
     * before the standalone Totem namespace. Canonical values always win.
     */
    private static final Map<String, String> LEGACY_KEY_MIGRATIONS = Map.ofEntries(
            Map.entry("deadrecall_activity", "totem_automata_activity"),
            Map.entry("deadrecall_blocked_bindings_hash", "totem_automata_blocked_bindings_hash"),
            Map.entry("deadrecall_blocked_source_container_dim", "totem_automata_blocked_source_container_dim"),
            Map.entry("deadrecall_blocked_source_container_x", "totem_automata_blocked_source_container_x"),
            Map.entry("deadrecall_blocked_source_container_y", "totem_automata_blocked_source_container_y"),
            Map.entry("deadrecall_blocked_source_container_z", "totem_automata_blocked_source_container_z"),
            Map.entry("deadrecall_blocked_source_hash", "totem_automata_blocked_source_hash"),
            Map.entry("deadrecall_blocked_targets_hash", "totem_automata_blocked_targets_hash"),
            Map.entry("deadrecall_bound_container_dim", "totem_automata_bound_container_dim"),
            Map.entry("deadrecall_bound_container_x", "totem_automata_bound_container_x"),
            Map.entry("deadrecall_bound_container_y", "totem_automata_bound_container_y"),
            Map.entry("deadrecall_bound_container_z", "totem_automata_bound_container_z"),
            Map.entry("deadrecall_bound_containers", "totem_automata_bound_containers"),
            Map.entry("deadrecall_data_version", "totem_automata_data_version"),
            Map.entry("deadrecall_fuel_stack", "totem_automata_fuel_stack"),
            Map.entry("deadrecall_fuel_ticks", "totem_automata_fuel_ticks"),
            Map.entry("deadrecall_gathering_area_dim", "totem_automata_gathering_area_dim"),
            Map.entry("deadrecall_gathering_break_required_ticks", "totem_automata_gathering_break_required_ticks"),
            Map.entry("deadrecall_gathering_break_state", "totem_automata_gathering_break_state"),
            Map.entry("deadrecall_gathering_break_ticks", "totem_automata_gathering_break_ticks"),
            Map.entry("deadrecall_gathering_corner_a_x", "totem_automata_gathering_corner_a_x"),
            Map.entry("deadrecall_gathering_corner_a_y", "totem_automata_gathering_corner_a_y"),
            Map.entry("deadrecall_gathering_corner_a_z", "totem_automata_gathering_corner_a_z"),
            Map.entry("deadrecall_gathering_corner_b_x", "totem_automata_gathering_corner_b_x"),
            Map.entry("deadrecall_gathering_corner_b_y", "totem_automata_gathering_corner_b_y"),
            Map.entry("deadrecall_gathering_corner_b_z", "totem_automata_gathering_corner_b_z"),
            Map.entry("deadrecall_gathering_llm_allowed_block_ids", "totem_automata_gathering_llm_allowed_block_ids"),
            Map.entry("deadrecall_gathering_llm_allowed_tags", "totem_automata_gathering_llm_allowed_tags"),
            Map.entry("deadrecall_gathering_llm_denied_block_ids", "totem_automata_gathering_llm_denied_block_ids"),
            Map.entry("deadrecall_gathering_llm_denied_tags", "totem_automata_gathering_llm_denied_tags"),
            Map.entry("deadrecall_gathering_llm_enabled", "totem_automata_gathering_llm_enabled"),
            Map.entry("deadrecall_gathering_llm_prompt", "totem_automata_gathering_llm_prompt"),
            Map.entry("deadrecall_gathering_llm_prompt_revision", "totem_automata_gathering_llm_prompt_revision"),
            Map.entry("deadrecall_gathering_llm_warmup_index", "totem_automata_gathering_llm_warmup_index"),
            Map.entry("deadrecall_gathering_manual_targets", "totem_automata_gathering_manual_targets"),
            Map.entry("deadrecall_gathering_nearest_scan_cursor", "totem_automata_gathering_nearest_scan_cursor"),
            Map.entry("deadrecall_gathering_nearest_scan_radius", "totem_automata_gathering_nearest_scan_radius"),
            Map.entry("deadrecall_gathering_retry_tick", "totem_automata_gathering_retry_tick"),
            Map.entry("deadrecall_gathering_scan_index", "totem_automata_gathering_scan_index"),
            Map.entry("deadrecall_gathering_skipped_targets", "totem_automata_gathering_skipped_targets"),
            Map.entry("deadrecall_gathering_storage_stack", "totem_automata_gathering_storage_stack"),
            Map.entry("deadrecall_gathering_target_x", "totem_automata_gathering_target_x"),
            Map.entry("deadrecall_gathering_target_y", "totem_automata_gathering_target_y"),
            Map.entry("deadrecall_gathering_target_z", "totem_automata_gathering_target_z"),
            Map.entry("deadrecall_gathering_tool_stack", "totem_automata_gathering_tool_stack"),
            Map.entry("deadrecall_last_operator_player", "totem_automata_last_operator_player"),
            Map.entry("deadrecall_llm_api_key", "totem_automata_llm_api_key"),
            Map.entry("deadrecall_llm_api_url", "totem_automata_llm_api_url"),
            Map.entry("deadrecall_llm_bindings", "totem_automata_llm_bindings"),
            Map.entry("deadrecall_llm_model", "totem_automata_llm_model"),
            Map.entry("deadrecall_mode", "totem_automata_mode"),
            Map.entry("deadrecall_revision", "totem_automata_revision"),
            Map.entry("deadrecall_sorting_blocked", "totem_automata_sorting_blocked"),
            Map.entry("deadrecall_source_copper_container_dim", "totem_automata_source_copper_container_dim"),
            Map.entry("deadrecall_source_copper_container_x", "totem_automata_source_copper_container_x"),
            Map.entry("deadrecall_source_copper_container_y", "totem_automata_source_copper_container_y"),
            Map.entry("deadrecall_source_copper_container_z", "totem_automata_source_copper_container_z"),
            Map.entry("deadrecall_source_slot", "totem_automata_source_slot"),
            Map.entry("deadrecall_transport_enabled", "totem_automata_transport_enabled"),
            Map.entry("deadrecall_tried_destinations", "totem_automata_tried_destinations")
    );
    private static final String LEGACY_STORAGE_SLOT_PREFIX = "deadrecall_gathering_storage_slot_";
    private static final String CANONICAL_STORAGE_SLOT_PREFIX = "totem_automata_gathering_storage_slot_";

    private CopperGolemData() {
    }

    /** Applies the legacy-compatible schema defaults and one-to-many binding migration. */
    public static boolean migrate(CompoundTag tag) {
        boolean changed = migrateLegacyKeys(tag);
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

    /** Reads an item with the registry context required by enchantment Components. */
    public static ItemStack readItemStack(CompoundTag tag, String key, RegistryAccess registryAccess) {
        return tag.read(key, ItemStack.OPTIONAL_CODEC, itemStackOps(registryAccess))
                .orElse(ItemStack.EMPTY)
                .copy();
    }

    /** Persists every ItemStack Component, including registry-backed enchantments. */
    public static void writeItemStack(CompoundTag tag, String key, ItemStack stack, RegistryAccess registryAccess) {
        if (stack.isEmpty()) {
            tag.remove(key);
        } else {
            tag.store(key, ItemStack.OPTIONAL_CODEC, itemStackOps(registryAccess), stack.copy());
        }
    }

    public static CompoundTag readEntityTag(Entity entity) {
        CustomData customData = entity.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = customData == null ? new CompoundTag() : customData.copyTag();
        if (migrate(tag)) {
            entity.setComponent(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
        return tag;
    }

    public static void writeEntityTag(Entity entity, CompoundTag tag) {
        migrate(tag);
        entity.setComponent(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private static boolean migrateLegacyKeys(CompoundTag tag) {
        boolean changed = false;
        for (Map.Entry<String, String> entry : LEGACY_KEY_MIGRATIONS.entrySet()) {
            changed |= migrateLegacyKey(tag, entry.getKey(), entry.getValue());
        }
        for (int slot = 0; slot < 16; slot++) {
            changed |= migrateLegacyKey(tag, LEGACY_STORAGE_SLOT_PREFIX + slot, CANONICAL_STORAGE_SLOT_PREFIX + slot);
        }
        return changed;
    }

    private static boolean migrateLegacyKey(CompoundTag tag, String legacyKey, String canonicalKey) {
        if (!tag.contains(legacyKey)) {
            return false;
        }
        if (!tag.contains(canonicalKey)) {
            var value = tag.get(legacyKey);
            if (value != null) {
                tag.put(canonicalKey, value.copy());
            }
        }
        tag.remove(legacyKey);
        return true;
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

    private static RegistryOps<Tag> itemStackOps(RegistryAccess registryAccess) {
        return RegistryOps.create(NbtOps.INSTANCE, registryAccess);
    }
}
