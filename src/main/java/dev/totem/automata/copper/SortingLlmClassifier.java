package dev.totem.automata.copper;

import net.minecraft.server.MinecraftServer;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Asynchronous sorting classifier that leaves current-state validation to its decision sink. */
public final class SortingLlmClassifier {
    private static final long FAILURE_RETRY_DELAY_MS = 60_000L;
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "TotemAutomata-SortingLLM");
        thread.setDaemon(true);
        return thread;
    });
    private static final LlmRequestGate REQUEST_GATE = new LlmRequestGate(FAILURE_RETRY_DELAY_MS, System::currentTimeMillis);

    private SortingLlmClassifier() {
    }

    public static void requestClassification(
            MinecraftServer server, UUID golemId, CopperGolemBinding binding, String itemId, String itemName,
            List<String> itemTags, String prompt, String apiUrl, String apiKey, String model,
            String referenceTable, SortingDecisionSink sink) {
        if (apiUrl == null || apiUrl.isBlank() || model == null || model.isBlank() || prompt == null || prompt.isBlank()) return;
        String normalizedPrompt = prompt.trim();
        String queryKey = LlmQueryKeys.sorting(golemId, binding, itemId, itemTags, normalizedPrompt);
        if (!REQUEST_GATE.tryStart(queryKey)) return;
        try {
            EXECUTOR.submit(() -> execute(server, golemId, binding, itemId, itemName, itemTags, normalizedPrompt,
                    apiUrl, apiKey, model, referenceTable, queryKey, sink));
        } catch (RuntimeException exception) {
            REQUEST_GATE.completeFailure(queryKey);
        }
    }

    public static void clearPendingRequests() { REQUEST_GATE.clear(); }

    private static void execute(
            MinecraftServer server, UUID golemId, CopperGolemBinding binding, String itemId, String itemName,
            List<String> itemTags, String prompt, String apiUrl, String apiKey, String model,
            String referenceTable, String queryKey, SortingDecisionSink sink) {
        try {
            LlmDecisionParser.Decision decision = CopperGolemLlmClient.askItemClassification(
                    apiUrl, apiKey, model, prompt, itemId, itemName, itemTags, referenceTable);
            server.execute(() -> {
                try {
                    sink.apply(server, golemId, binding, prompt, itemId, itemTags, decision);
                    REQUEST_GATE.completeSuccess(queryKey);
                } catch (RuntimeException exception) {
                    REQUEST_GATE.completeFailure(queryKey);
                }
            });
        } catch (Exception exception) {
            REQUEST_GATE.completeFailure(queryKey);
        }
    }
}
