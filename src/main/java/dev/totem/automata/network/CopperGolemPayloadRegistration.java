package dev.totem.automata.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import java.util.Objects;

/** Registers the first migrated Automata payload family exactly once per server graph. */
public final class CopperGolemPayloadRegistration {
    private static CopperGolemPayloadHandler registeredHandler;
    private CopperGolemPayloadRegistration() {
    }

    /**
     * Activates the complete serverbound receiver family exactly once. This is
     * intentionally not called by the additive module entrypoint.
     */
    public static synchronized void register(CopperGolemPayloadHandler handler) {
        Objects.requireNonNull(handler, "handler");
        if (registeredHandler != null) {
            if (registeredHandler != handler) {
                throw new IllegalStateException("Copper Golem payloads already registered by another authority");
            }
            return;
        }
        PayloadTypeRegistry.serverboundPlay().register(CopperGolemModePayload.TYPE, CopperGolemModePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(CopperGolemOperationPayload.TYPE, CopperGolemOperationPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(
                CopperGolemGatheringTargetPayload.TYPE, CopperGolemGatheringTargetPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(
                RequestCopperGolemVisualizationPayload.TYPE, RequestCopperGolemVisualizationPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(SaveCopperGolemLlmConfigPayload.TYPE, SaveCopperGolemLlmConfigPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(TestCopperGolemLlmConnectionPayload.TYPE, TestCopperGolemLlmConnectionPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(UpdateCopperGolemBindingLlmPayload.TYPE, UpdateCopperGolemBindingLlmPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(UpdateCopperGolemBindingCachePayload.TYPE, UpdateCopperGolemBindingCachePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(UpdateCopperGolemGatheringLlmPayload.TYPE, UpdateCopperGolemGatheringLlmPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(CopperGolemModePayload.TYPE,
                (payload, context) -> context.server().execute(() -> handler.setMode(context.player(), payload)));
        ServerPlayNetworking.registerGlobalReceiver(CopperGolemOperationPayload.TYPE,
                (payload, context) -> context.server().execute(() -> handler.setOperation(context.player(), payload)));
        ServerPlayNetworking.registerGlobalReceiver(CopperGolemGatheringTargetPayload.TYPE,
                (payload, context) -> context.server().execute(() -> handler.updateGatheringTarget(context.player(), payload)));
        ServerPlayNetworking.registerGlobalReceiver(RequestCopperGolemVisualizationPayload.TYPE,
                (payload, context) -> context.server().execute(() -> handler.requestVisualization(context.player(), payload)));
        ServerPlayNetworking.registerGlobalReceiver(SaveCopperGolemLlmConfigPayload.TYPE,
                (payload, context) -> context.server().execute(() -> handler.saveLlmConfig(context.player(), payload)));
        ServerPlayNetworking.registerGlobalReceiver(TestCopperGolemLlmConnectionPayload.TYPE,
                (payload, context) -> context.server().execute(() -> handler.testLlmConnection(context.player(), payload)));
        ServerPlayNetworking.registerGlobalReceiver(UpdateCopperGolemBindingLlmPayload.TYPE,
                (payload, context) -> context.server().execute(() -> handler.updateBindingLlm(context.player(), payload)));
        ServerPlayNetworking.registerGlobalReceiver(UpdateCopperGolemBindingCachePayload.TYPE,
                (payload, context) -> context.server().execute(() -> handler.updateBindingCache(context.player(), payload)));
        ServerPlayNetworking.registerGlobalReceiver(UpdateCopperGolemGatheringLlmPayload.TYPE,
                (payload, context) -> context.server().execute(() -> handler.updateGatheringLlm(context.player(), payload)));
        registeredHandler = handler;
    }

    /** Registers the legacy clientbound payload types with the future client cutover. */
    public static void registerClientboundTypes() {
        PayloadTypeRegistry.clientboundPlay().register(CopperWrenchBindingsPayload.TYPE, CopperWrenchBindingsPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(CopperGolemVisualizationPayload.TYPE, CopperGolemVisualizationPayload.CODEC);
    }
}
