package dev.totem.automata.client;

import dev.totem.automata.network.CopperWrenchBindingsPayload;

import java.util.UUID;
import java.util.function.Consumer;

/** Pending/current snapshot routing for the Copper Golem menu client UI. */
public final class CopperGolemMenuPayloadState {
    private CopperWrenchBindingsPayload pending;
    private UUID activeGolem;
    private Consumer<CopperWrenchBindingsPayload> activeConsumer;
    public void open(UUID golemId, Consumer<CopperWrenchBindingsPayload> consumer) { activeGolem = golemId; activeConsumer = consumer; if (pending != null && pending.golemId().equals(golemId)) { CopperWrenchBindingsPayload payload = pending; pending = null; consumer.accept(payload); } }
    public void close(Consumer<CopperWrenchBindingsPayload> consumer) { if (activeConsumer == consumer) { activeGolem = null; activeConsumer = null; } }
    public void receive(CopperWrenchBindingsPayload payload) { if (activeGolem != null && activeGolem.equals(payload.golemId()) && activeConsumer != null) activeConsumer.accept(payload); else pending = payload; }
}
