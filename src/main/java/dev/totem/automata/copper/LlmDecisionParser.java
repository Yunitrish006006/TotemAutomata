package dev.totem.automata.copper;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;

/** Parses a JSON-only LLM classification response into an allow-listed decision. */
public final class LlmDecisionParser {
    private LlmDecisionParser() {
    }

    public static Decision parse(String content, List<String> providedTags) {
        String normalized = extractJsonObject(stripReasoningBlocks(stripCodeFence(content.trim())));
        JsonObject json = JsonParser.parseString(normalized).getAsJsonObject();
        boolean matches = json.has("match") && json.get("match").getAsBoolean();
        List<String> tags = new ArrayList<>();
        JsonArray tagArray = json.has("tags") && json.get("tags").isJsonArray() ? json.getAsJsonArray("tags") : new JsonArray();
        for (JsonElement element : tagArray) {
            String tag = element.getAsString();
            if (providedTags.contains(tag) && !tags.contains(tag)) tags.add(tag);
        }
        return new Decision(matches, List.copyOf(tags));
    }

    private static String stripCodeFence(String content) {
        if (!content.startsWith("```")) return content;
        int firstNewline = content.indexOf('\n');
        int lastFence = content.lastIndexOf("```");
        return firstNewline >= 0 && lastFence > firstNewline ? content.substring(firstNewline + 1, lastFence).trim() : content;
    }

    private static String stripReasoningBlocks(String content) {
        String result = content;
        while (true) {
            String lower = result.toLowerCase();
            int start = lower.indexOf("<think>");
            if (start < 0) return result.trim();
            int end = lower.indexOf("</think>", start + 7);
            if (end < 0) return result.substring(0, start).trim();
            result = (result.substring(0, start) + result.substring(end + 8)).trim();
        }
    }

    private static String extractJsonObject(String content) {
        int start = content.indexOf('{');
        if (start < 0) return content;
        boolean inString = false;
        boolean escaping = false;
        int depth = 0;
        for (int index = start; index < content.length(); index++) {
            char character = content.charAt(index);
            if (escaping) { escaping = false; continue; }
            if (inString && character == '\\') { escaping = true; continue; }
            if (character == '"') { inString = !inString; continue; }
            if (inString) continue;
            if (character == '{') depth++;
            else if (character == '}' && --depth == 0) return content.substring(start, index + 1);
        }
        return content.substring(start);
    }

    public record Decision(boolean matches, List<String> tags) {
    }
}
