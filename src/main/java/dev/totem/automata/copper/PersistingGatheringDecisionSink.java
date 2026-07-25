package dev.totem.automata.copper;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.golem.CopperGolem;

import java.util.List;
import java.util.UUID;

/** Applies an asynchronous gathering classification only to the current persisted prompt revision. */
public final class PersistingGatheringDecisionSink implements GatheringDecisionSink {
    @Override
    public void apply(
            MinecraftServer server,
            UUID golemId,
            String blockId,
            List<String> blockTags,
            LlmDecisionParser.Decision decision,
            int promptRevision
    ) {
        CopperGolem golem = find(server, golemId);
        if (golem == null) {
            return;
        }
        CompoundTag tag = CopperGolemData.readEntityTag(golem);
        if (!GatheringLlmState.recordDecision(
                tag, blockId, blockTags, decision.matches(), decision.tags(), promptRevision)) {
            return;
        }
        GatheringRuntimeState.resetSearch(tag, true);
        CopperGolemData.writeEntityTag(golem, tag);
    }

    private static CopperGolem find(MinecraftServer server, UUID id) {
        for (var level : server.getAllLevels()) {
            Entity entity = level.getEntity(id);
            if (entity instanceof CopperGolem golem && !golem.isRemoved() && golem.isAlive()) {
                return golem;
            }
        }
        return null;
    }
}
