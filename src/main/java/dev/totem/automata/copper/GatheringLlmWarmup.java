package dev.totem.automata.copper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Bounded background classification of uncached gathering candidates while the golem is stopped. */
public final class GatheringLlmWarmup {
    private static final String WARMUP_INDEX = "deadrecall_gathering_llm_warmup_index";
    private static final int BUDGET_PER_TICK = 16;
    private static final GatheringDecisionSink DECISIONS = new PersistingGatheringDecisionSink();

    private GatheringLlmWarmup() {
    }

    public static void tick(CopperGolem golem, ServerLevel level) {
        CompoundTag tag = CopperGolemData.readEntityTag(golem);
        GatheringLlmState.Config llm = GatheringLlmState.read(tag);
        GolemLlmState.Config golemLlm = GolemLlmState.read(tag);
        if (!llm.usable(golemLlm)) {
            return;
        }
        var bounds = GatheringConfiguration.scanBounds(tag, level.dimension());
        var home = GatheringHomeResolver.resolve(tag, level);
        ItemStack tool = CopperGolemData.readItemStack(tag, "deadrecall_gathering_tool_stack", level.registryAccess());
        if (bounds.isEmpty() || home.isEmpty() || tool.isEmpty()) {
            return;
        }

        long volume = bounds.get().volume();
        long cursor = Math.floorMod(tag.getLongOr(WARMUP_INDEX, 0L), volume);
        int budget = (int) Math.min(BUDGET_PER_TICK, volume);
        List<String> manualTargets = GatheringConfiguration.manualTargets(tag);
        for (int offset = 0; offset < budget; offset++) {
            request(golem, level, tag, home.get().binding(), bounds.get(),
                    bounds.get().topDownPositionAt((cursor + offset) % volume), tool, manualTargets, llm, golemLlm);
        }
        CompoundTag updated = CopperGolemData.readEntityTag(golem);
        updated.putLong(WARMUP_INDEX, (cursor + budget) % volume);
        CopperGolemData.writeEntityTag(golem, updated);
    }

    private static void request(
            CopperGolem golem,
            ServerLevel level,
            CompoundTag tag,
            CopperGolemBinding home,
            GatheringScanCursor.Bounds bounds,
            BlockPos pos,
            ItemStack tool,
            List<String> manualTargets,
            GatheringLlmState.Config llm,
            GolemLlmState.Config golemLlm
    ) {
        if (!GatheringTargetPreconditions.eligible(level, bounds, home, CopperGolemData.readBindings(tag), pos)) {
            return;
        }
        var state = level.getBlockState(pos);
        var drops = GatheringDrops.resolve(golem, level, pos, state, tool);
        if (drops.isEmpty()) {
            return;
        }
        String blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        if (manualTargets.contains(blockId)) {
            return;
        }
        List<String> blockTags = GatheringLlmPromptData.blockTags(state);
        if (GatheringLlmState.cachedDecision(llm, blockId, blockTags).isPresent()) {
            return;
        }
        BlockLlmClassifier.requestClassification(
                level.getServer(), golem.getUUID(), blockId, state.getBlock().getName().getString(), blockTags,
                GatheringLlmPromptData.dropSummary(drops.get()), GatheringLlmPromptData.toolSummary(tool),
                llm.prompt(), llm.promptRevision(), golemLlm.apiUrl(), golemLlm.apiKey(), golemLlm.model(), DECISIONS);
    }
}
