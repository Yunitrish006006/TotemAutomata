package dev.totem.automata.client;

import dev.totem.automata.network.CopperGolemVisualizationPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** Registers clientbound Automata payloads once a visualization consumer is available. */
public final class CopperGolemClientPayloadRegistration {
    private static final AtomicBoolean REGISTERED = new AtomicBoolean();

    private CopperGolemClientPayloadRegistration() {
    }

    public static void register(Consumer<CopperGolemVisualizationPayload> visualizationConsumer) {
        if (!REGISTERED.compareAndSet(false, true)) return;
        PayloadTypeRegistry.clientboundPlay().register(
                CopperGolemVisualizationPayload.TYPE, CopperGolemVisualizationPayload.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(CopperGolemVisualizationPayload.TYPE,
                (payload, context) -> context.client().execute(() -> visualizationConsumer.accept(payload)));
    }
}
