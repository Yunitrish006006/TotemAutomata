package dev.totem.automata.copper;

import net.minecraft.nbt.CompoundTag;

/** Preserved per-golem OpenAI-compatible connection settings. */
public final class GolemLlmState {
    private static final String API_URL = "deadrecall_llm_api_url", API_KEY = "deadrecall_llm_api_key", MODEL = "deadrecall_llm_model";
    private GolemLlmState() { }
    public static Config read(CompoundTag tag) { return new Config(tag.getStringOr(API_URL, ""), tag.getStringOr(API_KEY, ""), tag.getStringOr(MODEL, "")); }
    public static void write(CompoundTag tag, Config value) {
        put(tag, API_URL, value.apiUrl()); put(tag, API_KEY, value.apiKey()); put(tag, MODEL, value.model());
    }
    private static void put(CompoundTag tag, String key, String value) { if (value == null || value.isBlank()) tag.remove(key); else tag.putString(key, value.trim()); }
    public record Config(String apiUrl, String apiKey, String model) {
        public boolean configured() { return !apiUrl.isBlank() && !model.isBlank(); }
    }
}
