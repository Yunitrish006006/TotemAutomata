package dev.totem.automata.network;

import net.minecraft.server.level.ServerPlayer;

/** Server-authoritative endpoint implementation supplied by the Automata behavior layer. */
public interface CopperGolemPayloadHandler {
    void setMode(ServerPlayer player, CopperGolemModePayload payload);

    void setOperation(ServerPlayer player, CopperGolemOperationPayload payload);

    void updateGatheringTarget(ServerPlayer player, CopperGolemGatheringTargetPayload payload);

    void requestVisualization(ServerPlayer player, RequestCopperGolemVisualizationPayload payload);

    void saveLlmConfig(ServerPlayer player, SaveCopperGolemLlmConfigPayload payload);

    void testLlmConnection(ServerPlayer player, TestCopperGolemLlmConnectionPayload payload);

    void updateBindingLlm(ServerPlayer player, UpdateCopperGolemBindingLlmPayload payload);

    void updateBindingCache(ServerPlayer player, UpdateCopperGolemBindingCachePayload payload);

    void updateGatheringLlm(ServerPlayer player, UpdateCopperGolemGatheringLlmPayload payload);
}
