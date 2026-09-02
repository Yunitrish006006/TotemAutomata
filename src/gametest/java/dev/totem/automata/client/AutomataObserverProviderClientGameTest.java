package dev.totem.automata.client;

import dev.totem.automata.menu.CopperGolemMenu;
import dev.totem.automata.menu.CopperGolemMenuOpenData;
import dev.totem.automata.menu.CopperGolemMenuRegistration;
import dev.totem.automata.network.CopperWrenchBindingsPayload;
import dev.totem.core.api.v1.client.observer.ObserverRemoteCursor;
import dev.totem.core.api.v1.client.observer.ObserverScreenContext;
import dev.totem.core.api.v1.client.observer.ObserverScreenHandle;
import dev.totem.core.api.v1.client.observer.ObserverScreenProvider;
import dev.totem.core.api.v1.client.observer.ObserverScreenSnapshot;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/** Owner-local runtime proof for the production Copper Golem Observer screen. */
@SuppressWarnings("UnstableApiUsage")
public final class AutomataObserverProviderClientGameTest implements FabricClientGameTest {
    private static final UUID GOLEM = UUID.fromString("00000000-0000-0000-0000-000000000042");

    @Override public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getClientLevel().waitForChunksRender();
            context.getInput().resizeWindow(1280, 720);
            AutomataObserverScreenProvider provider = context.computeOnClient(client -> {
                boolean registered = FabricLoader.getInstance()
                        .getEntrypoints(ObserverScreenProvider.ENTRYPOINT, ObserverScreenProvider.class).stream()
                        .anyMatch(AutomataObserverScreenProvider.class::isInstance);
                if (!registered) throw new AssertionError("Automata Observer provider entrypoint is missing");
                return new AutomataObserverScreenProvider();
            });
            ObserverScreenSnapshot initial = capture(context, provider, source(context, 7), 1);
            ObserverScreenSnapshot update = capture(context, provider, source(context, 8), 2);
            AtomicInteger stops = new AtomicInteger();
            ObserverScreenHandle handle = context.computeOnClient(client -> provider.create(
                    new ObserverScreenContext(GOLEM, "Target", stops::incrementAndGet), initial));
            context.runOnClient(client -> client.setScreenAndShow(handle.screen()));
            context.waitForScreen(CopperGolemMenuScreen.class);

            context.runOnClient(client -> {
                CopperGolemMenuScreen screen = (CopperGolemMenuScreen) handle.screen();
                require(screen.totem$isObserverReadOnly(),
                        "Automata production Screen did not enter Observer mode");
                require(screen.observerCaptureSource().orElseThrow().revision() == 7,
                        "Initial Observer snapshot was not applied");
                handle.applySnapshot(foreign(update, "automata_copper_golem", "wrong", 1, 90));
                handle.applySnapshot(foreign(update, "automata_copper_golem", "", 2, 91));
                handle.applySnapshot(foreign(update, "foreign", "", 1, 92));
                handle.applySnapshot(update);
                handle.applySnapshot(initial);
                require(screen.observerCaptureSource().orElseThrow().revision() == 8,
                        "Exact monotonic Automata snapshot policy failed");
                ItemStack carried = new ItemStack(Items.DIAMOND, 2);
                handle.applyCursor(new ObserverRemoteCursor(2, 88, 83, 176, 166, carried));
                handle.applyCursor(new ObserverRemoteCursor(1, 0, 0, 176, 166, ItemStack.EMPTY));
                require(ItemStack.matches(carried, screen.getMenu().getCarried()),
                        "Stale remote cursor replaced the carried stack");

                ObserverPacketProbe.reset();
                require(screen.mouseClicked(new MouseButtonEvent(1, 1,
                                new MouseButtonInfo(0, 0)), false),
                        "Observer mouse input was not consumed");
                require(screen.mouseScrolled(1, 1, 0, -1),
                        "Observer scroll input was not consumed");
                require(screen.keyPressed(new KeyEvent(65, 0, 0)),
                        "Observer keyboard input was not consumed");
                require(ObserverPacketProbe.sends() == 0,
                        "Observer input attempted a client packet");
            });
            context.waitTicks(2);
            context.takeScreenshot("automata-observer-owner-production-screen");
            context.runOnClient(client -> {
                ObserverPacketProbe.reset();
                require(handle.screen().keyPressed(new KeyEvent(256, 0, 0)),
                        "Escape was not consumed");
                require(stops.get() == 1, "Escape did not request stop-observing exactly once");
                require(ObserverPacketProbe.sends() == 0, "Closing Observer mode attempted a packet");
                client.setScreenAndShow(null);
            });
            context.waitForScreen(null);
        }
    }

    private static CopperGolemMenuScreen source(ClientGameTestContext context, int revision) {
        return context.computeOnClient(client -> {
            CopperGolemMenu menu = new CopperGolemMenu(CopperGolemMenuRegistration.TYPE, revision,
                    client.player.getInventory(), new CopperGolemMenuOpenData(GOLEM));
            CopperGolemMenuScreen screen = new CopperGolemMenuScreen(menu, client.player.getInventory(),
                    Component.translatable("container.deadrecall.copper_wrench.bindings"));
            screen.acceptSnapshotForVisualTest(snapshot(revision));
            return screen;
        });
    }

    private static ObserverScreenSnapshot capture(ClientGameTestContext context,
                                                  ObserverScreenProvider provider,
                                                  CopperGolemMenuScreen screen, long sequence) {
        return context.computeOnClient(client -> provider.capture(screen, sequence).orElseThrow());
    }

    private static ObserverScreenSnapshot foreign(ObserverScreenSnapshot source, String family,
                                                   String variant, int protocol, long sequence) {
        return new ObserverScreenSnapshot(family, variant, protocol, sequence, source.title(), source.slots(),
                source.data(), source.metadata(), source.ownerPayload());
    }

    private static CopperWrenchBindingsPayload snapshot(int revision) {
        CopperWrenchBindingsPayload.BindingEntry home = new CopperWrenchBindingsPayload.BindingEntry(
                "minecraft:overworld", 10, 64, 10, "minecraft:copper_chest", "minecraft:copper_chest",
                true, true, false, "", 0, 0, List.of(), List.of(), List.of(), List.of());
        return new CopperWrenchBindingsPayload(GOLEM, revision, true, "sorting", "searching",
                "minecraft:coal", 4, 800, false, "minecraft:air", 0, 0, 0,
                "minecraft:air", 0, "", "", "", 0, home, null, List.of(), false, "",
                0, 0, List.of(), List.of(), List.of(), List.of(), List.of());
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
