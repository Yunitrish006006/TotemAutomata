package dev.totem.automata.copper;

import net.minecraft.server.MinecraftServer;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Asynchronous, de-duplicated gathering block classification independent of handler internals. */
public final class BlockLlmClassifier {
    private static final long FAILURE_RETRY_DELAY_MS = 60_000L;
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "TotemAutomata-LLM");
        thread.setDaemon(true);
        return thread;
    });
    private static final LlmRequestGate REQUEST_GATE = new LlmRequestGate(FAILURE_RETRY_DELAY_MS, System::currentTimeMillis);

    private BlockLlmClassifier() {
    }

    public static void requestClassification(
            MinecraftServer server, UUID golemId, String blockId, String blockName, List<String> blockTags,
            List<String> expectedDrops, String toolSummary, String prompt, int promptRevision,
            String apiUrl, String apiKey, String model, GatheringDecisionSink sink) {
        if (apiUrl == null || apiUrl.isBlank() || model == null || model.isBlank() || prompt == null || prompt.isBlank()) return;
        String queryKey = LlmQueryKeys.gathering(golemId, blockId, blockTags, promptRevision);
        if (!REQUEST_GATE.tryStart(queryKey)) return;
        try {
            EXECUTOR.submit(() -> execute(server, golemId, blockId, blockName, blockTags, expectedDrops, toolSummary,
                    prompt, promptRevision, apiUrl, apiKey, model, queryKey, sink));
        } catch (RuntimeException exception) {
            REQUEST_GATE.completeFailure(queryKey);
        }
    }

    public static void clearPendingRequests() {
        REQUEST_GATE.clear();
    }

    private static void execute(
            MinecraftServer server, UUID golemId, String blockId, String blockName, List<String> blockTags,
            List<String> expectedDrops, String toolSummary, String prompt, int promptRevision,
            String apiUrl, String apiKey, String model, String queryKey, GatheringDecisionSink sink) {
        try {
            LlmDecisionParser.Decision decision = CopperGolemLlmClient.askBlockClassification(
                    apiUrl, apiKey, model, prompt, blockId, blockName, blockTags, expectedDrops, toolSummary);
            server.execute(() -> {
                try {
                    sink.apply(server, golemId, blockId, blockTags, decision, promptRevision);
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
