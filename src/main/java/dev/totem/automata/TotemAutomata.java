package dev.totem.automata;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Entry point for the optional Copper Golem automation module. */
public final class TotemAutomata implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("TotemAutomata");

    @Override
    public void onInitialize() {
        LOGGER.info("TotemAutomata initialized without Cognition dependency");
    }
}
