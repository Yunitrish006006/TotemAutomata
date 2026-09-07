package dev.totem.automata.copper;

import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;

import java.util.Objects;

/**
 * Explicit Fabric callback registration for the migrated Wrench authority.
 *
 * <p>The callback is registered exactly once by the production bootstrap.</p>
 */
public final class CopperWrenchCallbackRegistration {
    private static CopperWrenchInteractionAuthority registeredAuthority;
    private CopperWrenchCallbackRegistration() { }

    public static synchronized void register(CopperWrenchInteractionAuthority authority) {
        Objects.requireNonNull(authority, "authority");
        if (registeredAuthority != null) {
            if (registeredAuthority != authority) throw new IllegalStateException("Copper Wrench callbacks already registered");
            return;
        }
        AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) -> authority.attackBlock(player, level, hand, pos));
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> authority.useBlock(player, level, hand, hit.getBlockPos()));
        UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> authority.useEntity(player, level, hand, entity));
        registeredAuthority = authority;
    }
}
