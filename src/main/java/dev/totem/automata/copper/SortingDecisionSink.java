package dev.totem.automata.copper;

import net.minecraft.server.MinecraftServer;

import java.util.List;
import java.util.UUID;

/** Applies a completed sorting classification against the latest binding configuration. */
@FunctionalInterface
public interface SortingDecisionSink {
    void apply(MinecraftServer server, UUID golemId, CopperGolemBinding binding, String prompt,
               String itemId, List<String> itemTags, LlmDecisionParser.Decision decision);
}
