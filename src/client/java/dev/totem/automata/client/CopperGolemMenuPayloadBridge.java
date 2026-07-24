package dev.totem.automata.client;

import dev.totem.automata.network.CopperWrenchBindingsPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.Objects;
import java.util.function.Consumer;

/** Clientbound Wrench-menu payload receiver, activated with the migrated screen only. */
public final class CopperGolemMenuPayloadBridge {
    private static Consumer<CopperWrenchBindingsPayload> consumer;
    private CopperGolemMenuPayloadBridge() { }

    public static synchronized void register(Consumer<CopperWrenchBindingsPayload> receiver) {
        Objects.requireNonNull(receiver, "receiver");
        if (consumer != null) {
            if (consumer != receiver) throw new IllegalStateException("Copper Golem menu payload bridge already registered");
            return;
        }
        consumer = receiver;
        ClientPlayNetworking.registerGlobalReceiver(CopperWrenchBindingsPayload.TYPE,
                (payload, context) -> context.client().execute(() -> consumer.accept(payload)));
    }
}
