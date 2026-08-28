package dev.totem.automata.copper;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.golem.CopperGolem;

/** Module-owned behavior supplied to the generic Copper Golem controller. */
public interface CopperGolemBehavior {
    boolean shouldTrack(CopperGolem golem);

    void tick(MinecraftServer server, ServerLevel level, CopperGolem golem, boolean shouldPruneBindings);

    /** True only when this tick can consume lightweight gathering scan work. */
    default boolean needsScanBudget(CopperGolem golem) {
        return false;
    }

    /** Computes scheduler eligibility once for a loaded Golem. */
    default Scheduling scheduling(ServerLevel level, CopperGolem golem) {
        boolean shouldTick = shouldTrack(golem);
        return new Scheduling(shouldTick, shouldTick && needsScanBudget(golem));
    }

    /** Budget-aware controller seam; legacy test behaviors keep the original callback. */
    default void tick(
            MinecraftServer server,
            ServerLevel level,
            CopperGolem golem,
            boolean shouldPruneBindings,
            int scanBudget
    ) {
        tick(server, level, golem, shouldPruneBindings);
    }

    /** Budget-aware tick result used for deterministic controller diagnostics. */
    default TickResult tickScheduled(
            MinecraftServer server,
            ServerLevel level,
            CopperGolem golem,
            boolean shouldPruneBindings,
            int scanBudget
    ) {
        tick(server, level, golem, shouldPruneBindings, scanBudget);
        return TickResult.NONE;
    }

    record Scheduling(boolean shouldTick, boolean needsScanBudget) { }
    record TickResult(int scanPositionsInspected) {
        public static final TickResult NONE = new TickResult(0);

        public TickResult {
            if (scanPositionsInspected < 0) {
                throw new IllegalArgumentException("negative scan inspection count");
            }
        }
    }
}
