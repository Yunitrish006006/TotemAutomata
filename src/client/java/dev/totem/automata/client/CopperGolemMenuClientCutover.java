package dev.totem.automata.client;

import dev.totem.automata.network.CopperWrenchBindingsPayload;

import java.util.UUID;
import java.util.function.Consumer;

/** Explicit client half of the Copper Golem menu cutover; not called by the additive entrypoint. */
public final class CopperGolemMenuClientCutover {
    private static final CopperGolemMenuPayloadState STATE = new CopperGolemMenuPayloadState();
    private static boolean installed;
    private CopperGolemMenuClientCutover() { }
    public static synchronized void install() {
        if (installed) return;
        CopperGolemMenuPayloadBridge.register(STATE::receive);
        installed = true;
    }
    public static void open(UUID golemId, Consumer<CopperWrenchBindingsPayload> consumer) { STATE.open(golemId, consumer); }
    public static void close(Consumer<CopperWrenchBindingsPayload> consumer) { STATE.close(consumer); }
}
