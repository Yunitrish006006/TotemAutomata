package dev.totem.automata.copper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** Legacy Wrench source/destination confirmation path. */
public final class CopperWrenchPathVisualization {
    private CopperWrenchPathVisualization() { }
    public static void show(ServerLevel level, Entity golem, BlockPos target) {
        Vec3 start = golem.position().add(0, golem.getBbHeight() * .6D, 0), end = Vec3.atCenterOf(target);
        for (int i = 0; i <= 28; i++) { double t = i / 28D; level.sendParticles(ParticleTypes.WAX_ON, start.x + (end.x - start.x) * t, start.y + (end.y - start.y) * t, start.z + (end.z - start.z) * t, 1, 0, 0, 0, 0); }
    }
}
