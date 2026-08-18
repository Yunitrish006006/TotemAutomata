package dev.totem.automata;

import dev.totem.automata.bootstrap.AutomataServerCutoverComposition;
import dev.totem.automata.manual.AutomataManual;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Entry point for the version-gated Copper Golem automation cutover. */
public final class TotemAutomata implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("TotemAutomata");

    @Override
    public void onInitialize() {
        AutomataManual.register();
        AutomataServerCutoverComposition.activate();
        LOGGER.info("TotemAutomata cutover authority activated without Cognition dependency");
    }
}
