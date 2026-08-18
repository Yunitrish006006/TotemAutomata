package dev.totem.automata.copper;

import dev.totem.automata.network.CopperGolemGatheringTargetPayload;
import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.List;

/**
 * Server-authoritative mutations for the persisted Copper Golem schema.
 *
 * <p>This is deliberately entity-agnostic: the payload and callback runtime
 * validates its player/golem context, then reads and writes one tag through
 * this class. Keeping the mutation rules here prevents the pending external
 * payload authority from depending on DeadRecall's legacy Wrench handler.</p>
 */
public final class CopperGolemStateMutation {
    private static final String TRIED_DESTINATIONS = "deadrecall_tried_destinations";
    private static final String SOURCE_SLOT = "deadrecall_source_slot";
    private static final String GATHERING_TOOL = "deadrecall_gathering_tool_stack";
    private static final String GATHERING_STORAGE = "deadrecall_gathering_storage_stack";
    private static final List<String> SORTING_BLOCKED_KEYS = List.of(
            "deadrecall_sorting_blocked",
            "deadrecall_blocked_source_container_dim",
            "deadrecall_blocked_source_container_x",
            "deadrecall_blocked_source_container_y",
            "deadrecall_blocked_source_container_z",
            "deadrecall_blocked_source_hash",
            "deadrecall_blocked_bindings_hash",
            "deadrecall_blocked_targets_hash"
    );

    private CopperGolemStateMutation() {
    }

    /** Preserves the legacy operation toggle: it clears a stale sorting block and advances the revision. */
    public static void setTransportEnabled(CompoundTag tag, boolean enabled) {
        tag.putBoolean(CopperGolemData.TAG_TRANSPORT_ENABLED, enabled);
        clearSortingBlocked(tag);
        CopperGolemData.bumpRevision(tag);
    }

    /**
     * Returns whether an externally requested mode switch is safe for the
     * persisted state. The caller separately verifies that the golem is not
     * holding a sorting item before entering gathering mode.
     */
    public static boolean canSwitchMode(CompoundTag tag, boolean hasSortingHandItem) {
        if (tag.getBooleanOr(CopperGolemData.TAG_TRANSPORT_ENABLED, false)) {
            return false;
        }

        CopperGolemMode current = CopperGolemData.mode(tag);
        if (current == CopperGolemMode.SORTING) {
            return !hasSortingHandItem && !tag.contains(SOURCE_SLOT);
        }

        return !tag.contains(GATHERING_TOOL)
                && !tag.contains(GATHERING_STORAGE)
                && GatheringRuntimeState.target(tag).isEmpty();
    }

    /** Preserves the legacy transition cleanup while retaining the mode identifier. */
    public static void setMode(CompoundTag tag, CopperGolemMode mode) {
        tag.putString(CopperGolemData.TAG_MODE, mode.id());
        tag.remove(TRIED_DESTINATIONS);
        GatheringRuntimeState.resetSearch(tag, true);
        clearSortingBlocked(tag);
        CopperGolemData.bumpRevision(tag);
    }

    public static void configureGolemLlm(CompoundTag tag, String apiUrl, String apiKey, String model) {
        GolemLlmState.write(tag, new GolemLlmState.Config(apiUrl, apiKey, model));
        clearSortingBlocked(tag);
        CopperGolemData.bumpRevision(tag);
    }

    public static void configureBindingLlm(CompoundTag tag, CopperGolemBinding binding, boolean enabled, String prompt) {
        SortingLlmState.configure(tag, binding, enabled, prompt);
        clearSortingBlocked(tag);
        CopperGolemData.bumpRevision(tag);
    }

    public static void moveBindingLlmCache(
            CompoundTag tag,
            CopperGolemBinding binding,
            String value,
            boolean tagValue,
            boolean allowed
    ) {
        SortingLlmState.Config current = SortingLlmState.get(tag, binding);
        List<String> allowedItems = new ArrayList<>(current.allowedItemIds());
        List<String> deniedItems = new ArrayList<>(current.deniedItemIds());
        List<String> allowedTags = new ArrayList<>(current.allowedTags());
        List<String> deniedTags = new ArrayList<>(current.deniedTags());
        move(value, allowed, tagValue ? allowedTags : allowedItems, tagValue ? deniedTags : deniedItems);
        SortingLlmState.replace(tag, new SortingLlmState.Config(
                binding,
                current.enabled(),
                current.prompt(),
                allowedItems,
                deniedItems,
                allowedTags,
                deniedTags
        ));
        clearSortingBlocked(tag);
        CopperGolemData.bumpRevision(tag);
    }

    public static void configureGatheringLlm(CompoundTag tag, boolean enabled, String prompt) {
        GatheringLlmState.configure(tag, enabled, prompt);
        GatheringRuntimeState.resetSearch(tag, true);
        clearSortingBlocked(tag);
        CopperGolemData.bumpRevision(tag);
    }

    /** Removes one persisted gathering target/cache entry and preserves the legacy revision semantics. */
    public static boolean removeGatheringTarget(
            CompoundTag tag,
            String value,
            boolean tagValue,
            CopperGolemGatheringTargetPayload.TargetSet targetSet
    ) {
        boolean changed = switch (targetSet) {
            case MANUAL -> !tagValue && GatheringConfiguration.removeManualTarget(tag, value);
            case ALLOWED -> GatheringLlmState.removeCachedDecision(tag, value, tagValue, true);
            case DENIED -> GatheringLlmState.removeCachedDecision(tag, value, tagValue, false);
        };
        if (changed) {
            GatheringRuntimeState.resetSearch(tag, true);
            CopperGolemData.bumpRevision(tag);
        }
        return changed;
    }

    public static void clearSortingBlocked(CompoundTag tag) {
        for (String key : SORTING_BLOCKED_KEYS) {
            tag.remove(key);
        }
    }

    private static void move(String value, boolean allowed, List<String> allowedValues, List<String> deniedValues) {
        if (value == null || value.isBlank()) {
            return;
        }
        String normalized = value.trim();
        List<String> destination = allowed ? allowedValues : deniedValues;
        List<String> opposite = allowed ? deniedValues : allowedValues;
        if (!destination.contains(normalized)) {
            destination.add(normalized);
        }
        opposite.remove(normalized);
    }
}
