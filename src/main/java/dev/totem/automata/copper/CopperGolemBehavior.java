package dev.totem.automata.copper;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.golem.CopperGolem;

/** Module-owned behavior supplied to the generic Copper Golem controller. */
public interface CopperGolemBehavior {
    boolean shouldTrack(CopperGolem golem);

    void tick(MinecraftServer server, ServerLevel level, CopperGolem golem, boolean shouldPruneBindings);
}
