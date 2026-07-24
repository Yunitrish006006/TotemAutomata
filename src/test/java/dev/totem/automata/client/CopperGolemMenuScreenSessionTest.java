package dev.totem.automata.client;

import dev.totem.automata.network.CopperWrenchBindingsPayload;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class CopperGolemMenuScreenSessionTest {
    private static CopperWrenchBindingsPayload payload(UUID id) {
        var entry = new CopperWrenchBindingsPayload.BindingEntry("minecraft:overworld", 1, 2, 3, "minecraft:chest", "minecraft:chest", true, true, false, "", 0, 0, List.of(), List.of(), List.of(), List.of());
        return new CopperWrenchBindingsPayload(id, 2, false, "sorting", "stopped", "minecraft:coal", 1, 100, "minecraft:air", 0, 0, 0, "minecraft:air", 0, "", "", "", 0, null, null, List.of(), false, "", 0, 0, List.of(), List.of(), List.of(), List.of(), List.of(entry));
    }

    @Test void routesOnlyItsGolemSnapshotAndDispatchesControllerCommands() {
        UUID id = UUID.randomUUID(); AtomicReference<CopperGolemMenuClientController.OperationCommand> operation = new AtomicReference<>();
        var sender = new CopperGolemMenuScreenSession.CommandSender() {
            @Override public void operation(CopperGolemMenuClientController.OperationCommand c) { operation.set(c); }
            @Override public void mode(CopperGolemMenuClientController.ModeCommand c) { }
            @Override public void bindingLlm(CopperGolemMenuClientController.BindingLlmCommand c) { }
            @Override public void bindingCache(CopperGolemMenuClientController.BindingCacheCommand c) { }
            @Override public void gatheringLlm(CopperGolemMenuClientController.GatheringLlmCommand c) { }
            @Override public void apiConfig(CopperGolemMenuClientController.ApiConfigCommand c) { }
            @Override public void testApi(CopperGolemMenuClientController.TestApiCommand c) { }
            @Override public void gatheringTarget(CopperGolemMenuClientController.GatheringTargetCommand c) { }
        };
        var session = new CopperGolemMenuScreenSession(id, sender);
        session.accept(payload(UUID.randomUUID())); assertTrue(session.controller().snapshot().isEmpty());
        session.accept(payload(id)); session.toggleOperation();
        assertNotNull(operation.get()); assertTrue(operation.get().running()); assertEquals(id, operation.get().golemId());
    }
}
