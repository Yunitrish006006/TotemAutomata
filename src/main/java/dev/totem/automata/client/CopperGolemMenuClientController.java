package dev.totem.automata.client;

import dev.totem.automata.network.CopperWrenchBindingsPayload;
import dev.totem.automata.network.CopperGolemGatheringTargetPayload;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Rendering-independent Copper Golem menu controller.
 *
 * <p>A migrated screen can render {@link #snapshot()} and send the returned
 * command records through {@link CopperGolemMenuActions}.  It applies exactly
 * the same optimistic edits as the legacy screen but owns no Fabric receiver
 * or screen registration, so it remains safe while DeadRecall is authoritative.</p>
 */
public final class CopperGolemMenuClientController {
    private CopperWrenchBindingsPayload snapshot;

    public void apply(CopperWrenchBindingsPayload payload) { this.snapshot = Objects.requireNonNull(payload, "payload"); }
    public Optional<CopperWrenchBindingsPayload> snapshot() { return Optional.ofNullable(snapshot); }

    public Optional<OperationCommand> toggleOperation() {
        if (snapshot == null) return Optional.empty();
        boolean running = !snapshot.running();
        String activity = running ? (hasFuel(snapshot) ? "searching" : "blocked_no_fuel") : "stopped";
        snapshot = copy(snapshot, running, activity, snapshot.mode(), snapshot.llmApiUrl(), snapshot.llmApiKey(),
                snapshot.llmModel(), snapshot.gatheringLlmEnabled(), snapshot.gatheringLlmPrompt(), snapshot.bindings());
        return Optional.of(new OperationCommand(snapshot.golemId(), running, snapshot.revision()));
    }

    public Optional<ModeCommand> switchMode() {
        if (snapshot == null) return Optional.empty();
        String mode = "gathering".equals(normalizedMode(snapshot.mode())) ? "sorting" : "gathering";
        return Optional.of(new ModeCommand(snapshot.golemId(), mode, snapshot.revision()));
    }

    public Optional<BindingLlmCommand> updateBindingLlm(int index, boolean enabled, String prompt) {
        if (!validBinding(index)) return Optional.empty();
        CopperWrenchBindingsPayload.BindingEntry previous = snapshot.bindings().get(index);
        List<CopperWrenchBindingsPayload.BindingEntry> bindings = new ArrayList<>(snapshot.bindings());
        bindings.set(index, CopperGolemMenuEditor.updateBindingLlm(previous, enabled, prompt));
        snapshot = copy(snapshot, snapshot.running(), snapshot.activity(), snapshot.mode(), snapshot.llmApiUrl(), snapshot.llmApiKey(),
                snapshot.llmModel(), snapshot.gatheringLlmEnabled(), snapshot.gatheringLlmPrompt(), bindings);
        return Optional.of(new BindingLlmCommand(snapshot.golemId(), previous.dimension(), previous.x(), previous.y(), previous.z(),
                enabled, bindings.get(index).llmPrompt(), snapshot.revision()));
    }

    public Optional<BindingCacheCommand> moveCachedDecision(int index, String value, boolean tag, boolean allowed) {
        if (!validBinding(index) || value == null || value.isBlank()) return Optional.empty();
        CopperWrenchBindingsPayload.BindingEntry previous = snapshot.bindings().get(index);
        List<CopperWrenchBindingsPayload.BindingEntry> bindings = new ArrayList<>(snapshot.bindings());
        bindings.set(index, CopperGolemMenuEditor.moveCachedDecision(previous, value, tag, allowed));
        snapshot = copy(snapshot, snapshot.running(), snapshot.activity(), snapshot.mode(), snapshot.llmApiUrl(), snapshot.llmApiKey(),
                snapshot.llmModel(), snapshot.gatheringLlmEnabled(), snapshot.gatheringLlmPrompt(), bindings);
        return Optional.of(new BindingCacheCommand(snapshot.golemId(), previous.dimension(), previous.x(), previous.y(), previous.z(),
                value, tag, allowed, snapshot.revision()));
    }

    public Optional<GatheringLlmCommand> updateGatheringLlm(boolean enabled, String prompt) {
        if (snapshot == null) return Optional.empty();
        var settings = CopperGolemMenuEditor.updateGatheringLlm(enabled, prompt);
        snapshot = copy(snapshot, snapshot.running(), snapshot.activity(), snapshot.mode(), snapshot.llmApiUrl(), snapshot.llmApiKey(),
                snapshot.llmModel(), settings.enabled(), settings.prompt(), snapshot.bindings());
        return Optional.of(new GatheringLlmCommand(snapshot.golemId(), settings.enabled(), settings.prompt(), snapshot.revision()));
    }

    public Optional<ApiConfigCommand> saveApiConfig(String apiUrl, String apiKey, String model) {
        if (snapshot == null) return Optional.empty();
        String url = normalize(apiUrl), key = normalize(apiKey), selectedModel = normalize(model);
        snapshot = copy(snapshot, snapshot.running(), snapshot.activity(), snapshot.mode(), url, key, selectedModel,
                snapshot.gatheringLlmEnabled(), snapshot.gatheringLlmPrompt(), snapshot.bindings());
        return Optional.of(new ApiConfigCommand(snapshot.golemId(), url, key, selectedModel, snapshot.revision()));
    }
    public TestApiCommand testApiConnection(String apiUrl, String apiKey, String model) { return new TestApiCommand(normalize(apiUrl), normalize(apiKey), normalize(model)); }
    public Optional<GatheringTargetCommand> updateGatheringTarget(String value, boolean tag, CopperGolemGatheringTargetPayload.TargetSet set, CopperGolemGatheringTargetPayload.Action action) {
        if (snapshot == null || value == null || value.isBlank()) return Optional.empty();
        return Optional.of(new GatheringTargetCommand(snapshot.golemId(), value.trim(), tag, set, action, snapshot.revision()));
    }

    private boolean validBinding(int index) { return snapshot != null && index >= 0 && index < snapshot.bindings().size(); }
    private static boolean hasFuel(CopperWrenchBindingsPayload payload) { return payload.fuelCount() > 0 && payload.fuelTicks() > 0; }
    private static String normalizedMode(String mode) { return mode == null ? "sorting" : mode.trim().toLowerCase(java.util.Locale.ROOT); }
    private static String normalize(String value) { return value == null ? "" : value.trim(); }

    private static CopperWrenchBindingsPayload copy(CopperWrenchBindingsPayload p, boolean running, String activity, String mode,
            String apiUrl, String apiKey, String model, boolean gatheringLlmEnabled, String gatheringLlmPrompt,
            List<CopperWrenchBindingsPayload.BindingEntry> bindings) {
        return new CopperWrenchBindingsPayload(p.golemId(), p.revision(), running, mode, activity, p.fuelItemId(), p.fuelCount(),
                p.fuelTicks(), p.gatheringToolItemId(), p.gatheringToolCount(), p.gatheringToolDamage(), p.gatheringToolMaxDamage(),
                p.gatheringStorageItemId(), p.gatheringStorageCount(), apiUrl, apiKey, model, p.llmActiveCount(), p.sourceContainer(),
                p.gatheringArea(), p.gatheringManualTargets(), gatheringLlmEnabled, gatheringLlmPrompt, p.gatheringLlmCachedBlockIds(),
                p.gatheringLlmCachedTags(), p.gatheringLlmAllowedBlockIds(), p.gatheringLlmDeniedBlockIds(), p.gatheringLlmAllowedTags(),
                p.gatheringLlmDeniedTags(), List.copyOf(bindings));
    }

    public record OperationCommand(java.util.UUID golemId, boolean running, int revision) { }
    public record ModeCommand(java.util.UUID golemId, String mode, int revision) { }
    public record BindingLlmCommand(java.util.UUID golemId, String dimension, int x, int y, int z, boolean enabled, String prompt, int revision) { }
    public record BindingCacheCommand(java.util.UUID golemId, String dimension, int x, int y, int z, String value, boolean tag, boolean allowed, int revision) { }
    public record GatheringLlmCommand(java.util.UUID golemId, boolean enabled, String prompt, int revision) { }
    public record ApiConfigCommand(java.util.UUID golemId, String apiUrl, String apiKey, String model, int revision) { }
    public record TestApiCommand(String apiUrl, String apiKey, String model) { }
    public record GatheringTargetCommand(java.util.UUID golemId, String value, boolean tag, CopperGolemGatheringTargetPayload.TargetSet set, CopperGolemGatheringTargetPayload.Action action, int revision) { }
}
