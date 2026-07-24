package dev.totem.automata.copper;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.golem.CopperGolem;

import java.util.List;
import java.util.UUID;

/** Applies asynchronous sorting decisions only when the original binding prompt is still current. */
public final class PersistingSortingDecisionSink implements SortingDecisionSink {
    @Override public void apply(MinecraftServer server, UUID golemId, CopperGolemBinding binding, String prompt,
                                String itemId, List<String> itemTags, LlmDecisionParser.Decision decision) {
        CopperGolem golem = find(server, golemId);
        if (golem == null) return;
        CompoundTag tag = CopperGolemData.readEntityTag(golem);
        if (!CopperGolemData.readBindings(tag).contains(binding)) return;
        SortingLlmState.Config current = SortingLlmState.get(tag, binding);
        if (!current.enabled() || !current.prompt().equals(prompt)) return;
        SortingLlmState.recordDecision(tag, binding, itemId, itemTags, decision.matches(), decision.tags());
        CopperGolemData.writeEntityTag(golem, tag);
    }
    private static CopperGolem find(MinecraftServer server, UUID id) {
        for (var level : server.getAllLevels()) {
            Entity entity = level.getEntity(id);
            if (entity instanceof CopperGolem golem && !golem.isRemoved() && golem.isAlive()) return golem;
        }
        return null;
    }
}
