package dev.totem.automata.copper;

import net.minecraft.server.MinecraftServer;

import java.util.List;
import java.util.UUID;

/** Applies a completed gathering classification using the current server-authoritative state. */
@FunctionalInterface
public interface GatheringDecisionSink {
    void apply(MinecraftServer server, UUID golemId, String blockId, List<String> blockTags,
               LlmDecisionParser.Decision decision, int promptRevision);
}
