package dev.totem.automata.client;

import dev.totem.automata.menu.CopperGolemMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Renderer-lifecycle base state for the pending concrete Automata menu screen. */
public final class CopperGolemMenuScreenLifecycle {
    private final CopperGolemMenu menu;
    private final Inventory inventory;
    private final Component title;
    private final CopperGolemMenuScreenSession session;
    public CopperGolemMenuScreenLifecycle(CopperGolemMenu menu, Inventory inventory, Component title) {
        this.menu = menu; this.inventory = inventory; this.title = title; this.session = new CopperGolemMenuScreenSession(menu.golemId());
    }
    public void open() { session.open(); }
    public void close() { session.close(); }
    public CopperGolemMenu menu() { return menu; }
    public Inventory inventory() { return inventory; }
    public Component title() { return title; }
    public CopperGolemMenuScreenSession session() { return session; }
}
