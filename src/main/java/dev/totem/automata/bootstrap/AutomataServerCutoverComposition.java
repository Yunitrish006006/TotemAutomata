package dev.totem.automata.bootstrap;

import dev.totem.automata.advancement.AutomataCriteria;
import dev.totem.automata.copper.CopperGolemController;
import dev.totem.automata.copper.CopperGolemLifecycleRegistration;
import dev.totem.automata.copper.CopperWrenchCallbackRegistration;
import dev.totem.automata.copper.PersistedCopperGolemRuntime;
import dev.totem.automata.copper.PersistedCopperWrenchInteractionAuthority;
import dev.totem.automata.menu.PersistedCopperGolemMenuOpener;
import dev.totem.automata.network.CopperGolemPayloadRegistration;
import dev.totem.automata.network.PersistedCopperGolemPayloadHandler;
import dev.totem.automata.network.PersistedCopperGolemSnapshotSender;
import dev.totem.automata.registry.AutomataRegistries;

/**
 * The single server-side activation point for the future Automata cutover.
 *
 * <p>No current entrypoint invokes this. The bundle gate must first remove
 * every equivalent DeadRecall registration and enable the matching external
 * mixin/client compositions in the same release.</p>
 */
public final class AutomataServerCutoverComposition {
    private static boolean activated;

    private AutomataServerCutoverComposition() {
    }

    public static synchronized void activate() {
        if (activated) {
            return;
        }

        AutomataRegistries.registerCriteria();
        AutomataRegistries.registerMenus();
        AutomataRegistries.registerItems();
        AutomataRegistries.registerCreativeTabEntry();

        PersistedCopperGolemSnapshotSender snapshots = new PersistedCopperGolemSnapshotSender();
        CopperGolemPayloadRegistration.registerClientboundTypes();
        CopperGolemPayloadRegistration.register(new PersistedCopperGolemPayloadHandler(snapshots));
        CopperWrenchCallbackRegistration.register(new PersistedCopperWrenchInteractionAuthority(
                new PersistedCopperGolemMenuOpener(snapshots),
                AutomataCriteria.FIRST_COPPER_GOLEM_BINDING::trigger));
        CopperGolemLifecycleRegistration.register(new CopperGolemController(), new PersistedCopperGolemRuntime());
        activated = true;
    }
}
