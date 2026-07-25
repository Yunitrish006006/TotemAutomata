package dev.totem.automata.client;

import net.fabricmc.api.ClientModInitializer;

/** Client entry point paired with the server-side 0.1.1 Automata cutover. */
public final class TotemAutomataClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        AutomataClientCutoverComposition.activate();
    }
}
