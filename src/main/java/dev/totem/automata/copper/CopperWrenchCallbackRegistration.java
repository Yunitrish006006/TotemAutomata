package dev.totem.automata.copper;

import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;

import java.util.Objects;

/**
 * Explicit Fabric callback registration for the migrated Wrench authority.
 *
 * <p>It is intentionally not invoked by the additive entrypoint: registering
 * it before the full authority move would make both mods handle the same
 * interaction.  The cutover invokes it exactly once after DeadRecall's
 * legacy handler is removed.</p>
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
