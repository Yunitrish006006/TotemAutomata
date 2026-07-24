package dev.totem.automata.copper;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Minimal OpenAI-compatible, non-streaming chat-completions transport. */
public final class LlmChatClient {
    private static final int CONNECT_TIMEOUT_MS = 5_000;
    private static final int READ_TIMEOUT_MS = 15_000;

    private LlmChatClient() {
    }

    public static String complete(String apiUrl, String apiKey, String model, int maxTokens, List<Message> messages)
            throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("model", model);
        body.addProperty("temperature", 0);
        body.addProperty("max_tokens", maxTokens);
        body.addProperty("stream", false);
        JsonArray jsonMessages = new JsonArray();
        for (Message message : messages) {
            JsonObject jsonMessage = new JsonObject();
            jsonMessage.addProperty("role", message.role());
            jsonMessage.addProperty("content", message.content());
            jsonMessages.add(jsonMessage);
        }
        body.add("messages", jsonMessages);
        JsonObject response = post(apiUrl, apiKey, body);
        JsonArray choices = response.getAsJsonArray("choices");
        if (choices == null || choices.isEmpty()) throw new IllegalStateException("LLM response has no choices");
        JsonObject message = choices.get(0).getAsJsonObject().getAsJsonObject("message");
        return message == null || !message.has("content") ? "" : message.get("content").getAsString();
    }

    private static JsonObject post(String apiUrl, String apiKey, JsonObject body) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(apiUrl).openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        if (apiKey != null && !apiKey.isBlank()) connection.setRequestProperty("Authorization", "Bearer " + apiKey);
        connection.setDoOutput(true);
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        connection.getOutputStream().write(body.toString().getBytes(StandardCharsets.UTF_8));
        int status = connection.getResponseCode();
        InputStream stream = status >= 200 && status < 300 ? connection.getInputStream() : connection.getErrorStream();
        String response = stream == null ? "" : new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        connection.disconnect();
        if (status < 200 || status >= 300) throw new IllegalStateException("HTTP " + status + ": " + response);
        return JsonParser.parseString(response).getAsJsonObject();
    }

    public record Message(String role, String content) {
    }
}
