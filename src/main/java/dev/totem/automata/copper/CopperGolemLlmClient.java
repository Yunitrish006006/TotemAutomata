package dev.totem.automata.copper;

import java.util.List;

/** Copper Golem-specific prompts over the generic OpenAI-compatible transport. */
public final class CopperGolemLlmClient {
    private CopperGolemLlmClient() {
    }

    public static LlmDecisionParser.Decision askItemClassification(
            String apiUrl, String apiKey, String model, String prompt, String itemId, String itemName,
            List<String> itemTags, String referenceTable) throws Exception {
        String content = LlmChatClient.complete(apiUrl, apiKey, model, 256, List.of(
                new LlmChatClient.Message("system", """
                        You are a Minecraft item sorting classifier.
                        Return only JSON with this schema:
                        {"match":true|false,"tags":["tag_id"]}
                        The tags array must contain only tag ids from the provided item tags that are useful for future matching.
                        If no provided tag is useful, return an empty tags array.
                        Do not include markdown, explanations, or thinking text.
                        """ + referenceTable),
                new LlmChatClient.Message("user", String.format(
                        "Container prompt: %s%nItem id: %s%nItem name: %s%nItem tags: %s%nShould this item be sorted into this container?%n/no_think",
                        prompt, itemId, itemName, itemTags))));
        return LlmDecisionParser.parse(content, itemTags);
    }

    public static LlmDecisionParser.Decision askBlockClassification(
            String apiUrl, String apiKey, String model, String prompt, String blockId, String blockName,
            List<String> blockTags, List<String> expectedDrops, String toolSummary) throws Exception {
        String content = LlmChatClient.complete(apiUrl, apiKey, model, 256, List.of(
                new LlmChatClient.Message("system", """
                        You are a Minecraft block gathering classifier for a copper golem.
                        Return only JSON with this schema:
                        {"match":true|false,"tags":["tag_id"]}
                        The tags array must contain only tag ids from the provided block tags that are useful for future matching.
                        If no provided tag is useful, return an empty tags array.
                        The mod already rejected unsafe blocks, containers, unbreakable blocks, wrong tools, and drops that cannot fit.
                        Do not include markdown, explanations, or thinking text.
                        """),
                new LlmChatClient.Message("user", String.format(
                        "Gathering prompt: %s%nBlock id: %s%nBlock name: %s%nBlock tags: %s%nExpected drops: %s%nTool: %s%nShould this block type be gathered?%n/no_think",
                        prompt, blockId, blockName, blockTags, expectedDrops, toolSummary))));
        return LlmDecisionParser.parse(content, blockTags);
    }

    public static void askConnectionTest(String apiUrl, String apiKey, String model) throws Exception {
        LlmChatClient.complete(apiUrl, apiKey, model, 64, List.of(
                new LlmChatClient.Message("system", "Reply with exactly OK. Do not include thinking text."),
                new LlmChatClient.Message("user", "Connection test. Reply OK. /no_think")));
    }
}
