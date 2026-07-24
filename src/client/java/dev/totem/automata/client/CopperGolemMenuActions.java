package dev.totem.automata.client;

import dev.totem.automata.network.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.UUID;

/** Serverbound Copper Golem menu actions; all requests retain the legacy payload IDs. */
public final class CopperGolemMenuActions {
    private CopperGolemMenuActions() { }
    public static void operation(UUID id, boolean running, int revision) { send(CopperGolemOperationPayload.TYPE, new CopperGolemOperationPayload(id, running, revision)); }
    public static void mode(UUID id, String mode, int revision) { send(CopperGolemModePayload.TYPE, new CopperGolemModePayload(id, mode, revision)); }
    public static void saveApi(UUID id, String url, String key, String model, int revision) { send(SaveCopperGolemLlmConfigPayload.TYPE, new SaveCopperGolemLlmConfigPayload(id, url, key, model, revision)); }
    public static void testApi(String url, String key, String model) { send(TestCopperGolemLlmConnectionPayload.TYPE, new TestCopperGolemLlmConnectionPayload(url, key, model)); }
    public static void bindingLlm(UUID id, String dimension, int x, int y, int z, boolean enabled, String prompt, int revision) { send(UpdateCopperGolemBindingLlmPayload.TYPE, new UpdateCopperGolemBindingLlmPayload(id, dimension, x, y, z, enabled, prompt, revision)); }
    public static void bindingCache(UUID id, String dimension, int x, int y, int z, String value, boolean tag, boolean allowed, int revision) { send(UpdateCopperGolemBindingCachePayload.TYPE, new UpdateCopperGolemBindingCachePayload(id, dimension, x, y, z, value, tag, allowed, revision)); }
    public static void gatheringLlm(UUID id, boolean enabled, String prompt, int revision) { send(UpdateCopperGolemGatheringLlmPayload.TYPE, new UpdateCopperGolemGatheringLlmPayload(id, enabled, prompt, revision)); }
    public static void gatheringTarget(UUID id, String value, boolean tag, CopperGolemGatheringTargetPayload.TargetSet set, CopperGolemGatheringTargetPayload.Action action, int revision) { send(CopperGolemGatheringTargetPayload.TYPE, new CopperGolemGatheringTargetPayload(id, value, tag, set, action, revision)); }
    private static <T extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> void send(net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<T> type, T payload) { if (ClientPlayNetworking.canSend(type)) ClientPlayNetworking.send(payload); }
}
