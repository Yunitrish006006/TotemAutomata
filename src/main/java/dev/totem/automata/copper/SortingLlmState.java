package dev.totem.automata.copper;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

import java.util.ArrayList;
import java.util.List;

/** Preserved NBT codec and mutation rules for per-binding sorting LLM state. */
public final class SortingLlmState {
    private static final String CONFIGS = "deadrecall_llm_bindings", ENABLED = "llm_enabled", PROMPT = "llm_prompt";
    private static final String ALLOWED_ITEMS = "llm_allowed_item_ids", DENIED_ITEMS = "llm_denied_item_ids",
            ALLOWED_TAGS = "llm_allowed_tags", DENIED_TAGS = "llm_denied_tags";
    private static final int LIMIT = 128;
    private SortingLlmState() { }

    public static Config get(CompoundTag tag, CopperGolemBinding binding) {
        return read(tag).stream().filter(value -> value.binding().equals(binding)).findFirst().orElse(Config.empty(binding));
    }
    public static List<Config> read(CompoundTag tag) {
        List<Config> values = new ArrayList<>();
        tag.getList(CONFIGS).ifPresent(list -> list.compoundStream().forEach(value ->
                CopperGolemData.readBinding(value, CopperGolemData.TAG_BINDING_DIM, CopperGolemData.TAG_BINDING_X, CopperGolemData.TAG_BINDING_Y, CopperGolemData.TAG_BINDING_Z)
                        .ifPresent(binding -> values.add(new Config(binding, value.getBooleanOr(ENABLED, false), value.getStringOr(PROMPT, ""),
                                strings(value, ALLOWED_ITEMS), strings(value, DENIED_ITEMS), strings(value, ALLOWED_TAGS), strings(value, DENIED_TAGS))))));
        return List.copyOf(values);
    }
    public static void write(CompoundTag tag, List<Config> configs) {
        ListTag list = new ListTag();
        for (Config config : configs) {
            if (!config.keep()) continue;
            CompoundTag value = new CompoundTag();
            CopperGolemData.writeBinding(value, config.binding(), CopperGolemData.TAG_BINDING_DIM, CopperGolemData.TAG_BINDING_X, CopperGolemData.TAG_BINDING_Y, CopperGolemData.TAG_BINDING_Z);
            value.putBoolean(ENABLED, config.enabled()); value.putString(PROMPT, config.prompt());
            putStrings(value, ALLOWED_ITEMS, config.allowedItemIds()); putStrings(value, DENIED_ITEMS, config.deniedItemIds());
            putStrings(value, ALLOWED_TAGS, config.allowedTags()); putStrings(value, DENIED_TAGS, config.deniedTags()); list.add(value);
        }
        if (list.isEmpty()) tag.remove(CONFIGS); else tag.put(CONFIGS, list);
    }
    public static void configure(CompoundTag tag, CopperGolemBinding binding, boolean enabled, String prompt) {
        Config current = get(tag, binding); String normalized = prompt == null ? "" : prompt.trim(); boolean changed = !normalized.equals(current.prompt());
        replace(tag, new Config(binding, enabled, normalized, changed ? List.of() : current.allowedItemIds(), changed ? List.of() : current.deniedItemIds(),
                changed ? List.of() : current.allowedTags(), changed ? List.of() : current.deniedTags()));
    }
    public static void recordDecision(CompoundTag tag, CopperGolemBinding binding, String itemId, List<String> itemTags, boolean allowed, List<String> acceptedTags) {
        Config current = get(tag, binding); List<String> yesItems = new ArrayList<>(current.allowedItemIds()), noItems = new ArrayList<>(current.deniedItemIds());
        List<String> yesTags = new ArrayList<>(current.allowedTags()), noTags = new ArrayList<>(current.deniedTags()); move(itemId, allowed, yesItems, noItems);
        for (String itemTag : itemTags) { if (acceptedTags.contains(itemTag)) move(itemTag, allowed, yesTags, noTags); }
        replace(tag, new Config(binding, current.enabled(), current.prompt(), yesItems, noItems, yesTags, noTags));
    }
    public static void replace(CompoundTag tag, Config config) {
        List<Config> configs = new ArrayList<>(read(tag)); for (int i = 0; i < configs.size(); i++) if (configs.get(i).binding().equals(config.binding())) { configs.set(i, config); write(tag, configs); return; }
        configs.add(config); write(tag, configs);
    }
    private static List<String> strings(CompoundTag tag, String key) { return tag.getList(key).map(list -> list.stream().map(value -> value.asString().orElse("")).filter(value -> !value.isBlank()).limit(LIMIT).toList()).orElse(List.of()); }
    private static void putStrings(CompoundTag tag, String key, List<String> values) { ListTag list = new ListTag(); values.stream().filter(value -> value != null && !value.isBlank()).distinct().limit(LIMIT).forEach(value -> list.add(net.minecraft.nbt.StringTag.valueOf(value))); if (list.isEmpty()) tag.remove(key); else tag.put(key, list); }
    private static void move(String value, boolean allowed, List<String> yes, List<String> no) { if (value == null || value.isBlank()) return; List<String> target = allowed ? yes : no, other = allowed ? no : yes; if (!target.contains(value)) target.add(value); other.remove(value); }
    public record Config(CopperGolemBinding binding, boolean enabled, String prompt, List<String> allowedItemIds, List<String> deniedItemIds, List<String> allowedTags, List<String> deniedTags) {
        static Config empty(CopperGolemBinding binding) { return new Config(binding, false, "", List.of(), List.of(), List.of(), List.of()); }
        boolean keep() { return enabled || !prompt.isBlank() || !allowedItemIds.isEmpty() || !deniedItemIds.isEmpty() || !allowedTags.isEmpty() || !deniedTags.isEmpty(); }
    }
}
