package dev.totem.automata.copper;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.world.entity.animal.golem.CopperGolem;

import java.util.Objects;

/**
 * One-shot Fabric lifecycle registration for a fully composed Automata
 * authority. It is deliberately not called by the additive entrypoint.
 */
public final class CopperGolemLifecycleRegistration {
    private static CopperGolemController registeredController;

    private CopperGolemLifecycleRegistration() {
    }

    /**
     * Registers death cleanup and the supplied persisted runtime exactly once.
     * The caller must have gated DeadRecall's corresponding callbacks first.
     */
    public static synchronized void register(CopperGolemController controller, CopperGolemBehavior behavior) {
        Objects.requireNonNull(controller, "controller");
        Objects.requireNonNull(behavior, "behavior");
        if (registeredController != null) {
            if (registeredController != controller) {
                throw new IllegalStateException("Copper Golem lifecycle already registered");
            }
            return;
        }

        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, damageSource, damageAmount) -> {
            if (entity instanceof CopperGolem golem) {
                CopperGolemLifecycle.clearGatheringDisplayedItem(golem);
            }
            return true;
        });
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (entity instanceof CopperGolem golem) {
                CopperGolemLifecycle.dropGatheringInventory(golem);
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> controller.tick(server, behavior));
        registeredController = controller;
    }
}
