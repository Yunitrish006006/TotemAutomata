package dev.totem.automata.copper;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Legacy-compatible gathering LLM configuration, revisioning, and decision cache. */
public final class GatheringLlmState {
    private static final String ENABLED = "deadrecall_gathering_llm_enabled", PROMPT = "deadrecall_gathering_llm_prompt", REVISION = "deadrecall_gathering_llm_prompt_revision";
    private static final String ALLOWED_BLOCKS = "deadrecall_gathering_llm_allowed_block_ids", DENIED_BLOCKS = "deadrecall_gathering_llm_denied_block_ids", ALLOWED_TAGS = "deadrecall_gathering_llm_allowed_tags", DENIED_TAGS = "deadrecall_gathering_llm_denied_tags";
    private static final int LIMIT = 128;
    private GatheringLlmState() { }

    public static Config read(CompoundTag tag) { return new Config(tag.getBooleanOr(ENABLED, false), tag.getStringOr(PROMPT, ""), tag.getIntOr(REVISION, 0), strings(tag, ALLOWED_BLOCKS), strings(tag, DENIED_BLOCKS), strings(tag, ALLOWED_TAGS), strings(tag, DENIED_TAGS)); }
    public static void configure(CompoundTag tag, boolean enabled, String prompt) {
        Config current = read(tag); String normalized = normalize(prompt); boolean changed = !normalized.equals(current.prompt());
        tag.putBoolean(ENABLED, enabled); put(tag, PROMPT, normalized);
        if (changed) { tag.putInt(REVISION, current.promptRevision() + 1); clearCache(tag); }
    }
    public static boolean recordDecision(CompoundTag tag, String blockId, List<String> blockTags, boolean allowed, List<String> acceptedTags, int promptRevision) {
        Config current = read(tag); if (promptRevision != current.promptRevision()) return false;
        List<String> yesBlocks = new ArrayList<>(current.allowedBlockIds()), noBlocks = new ArrayList<>(current.deniedBlockIds());
        List<String> yesTags = new ArrayList<>(current.allowedTags()), noTags = new ArrayList<>(current.deniedTags());
        move(blockId, allowed, yesBlocks, noBlocks);
        for (String tagId : blockTags) if (acceptedTags.contains(tagId)) move(tagId, allowed, yesTags, noTags);
        write(tag, ALLOWED_BLOCKS, yesBlocks); write(tag, DENIED_BLOCKS, noBlocks); write(tag, ALLOWED_TAGS, yesTags); write(tag, DENIED_TAGS, noTags); return true;
    }
    public static boolean removeCachedDecision(CompoundTag tag, String value, boolean tagValue, boolean allowed) {
        String normalized = normalize(value); if (normalized.isBlank()) return false; Config c = read(tag);
        List<String> values = new ArrayList<>(tagValue ? (allowed ? c.allowedTags() : c.deniedTags()) : (allowed ? c.allowedBlockIds() : c.deniedBlockIds()));
        if (!values.remove(normalized)) return false;
        write(tag, tagValue ? (allowed ? ALLOWED_TAGS : DENIED_TAGS) : (allowed ? ALLOWED_BLOCKS : DENIED_BLOCKS), values); return true;
    }
    public static Optional<Boolean> cachedDecision(Config c, String blockId, List<String> blockTags) {
        if (c.allowedBlockIds().contains(blockId)) return Optional.of(true); if (c.deniedBlockIds().contains(blockId)) return Optional.of(false);
        for (String tag : blockTags) { if (c.allowedTags().contains(tag)) return Optional.of(true); if (c.deniedTags().contains(tag)) return Optional.of(false); }
        return Optional.empty();
    }
    public static void clearCache(CompoundTag tag) { tag.remove(ALLOWED_BLOCKS); tag.remove(DENIED_BLOCKS); tag.remove(ALLOWED_TAGS); tag.remove(DENIED_TAGS); }
    private static List<String> strings(CompoundTag tag, String key) { return tag.getList(key).map(list -> list.stream().map(value -> value.asString().orElse("")).filter(value -> !value.isBlank()).limit(LIMIT).toList()).orElse(List.of()); }
    private static void write(CompoundTag tag, String key, List<String> values) { ListTag list = new ListTag(); values.stream().filter(value -> value != null && !value.isBlank()).distinct().limit(LIMIT).forEach(value -> list.add(StringTag.valueOf(value))); if (list.isEmpty()) tag.remove(key); else tag.put(key, list); }
    private static void put(CompoundTag tag, String key, String value) { if (value.isBlank()) tag.remove(key); else tag.putString(key, value); }
    private static void move(String value, boolean allowed, List<String> yes, List<String> no) { if (value == null || value.isBlank()) return; List<String> to = allowed ? yes : no, from = allowed ? no : yes; if (!to.contains(value)) to.add(value); from.remove(value); }
    private static String normalize(String value) { return value == null ? "" : value.trim(); }
    public record Config(boolean enabled, String prompt, int promptRevision, List<String> allowedBlockIds, List<String> deniedBlockIds, List<String> allowedTags, List<String> deniedTags) {
        public boolean usable(GolemLlmState.Config golem) { return enabled && !prompt.isBlank() && golem.configured(); }
    }
}
