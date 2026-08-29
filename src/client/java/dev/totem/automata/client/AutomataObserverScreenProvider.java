package dev.totem.automata.client;

import dev.totem.automata.menu.CopperGolemMenu;
import dev.totem.automata.menu.CopperGolemMenuOpenData;
import dev.totem.automata.menu.CopperGolemMenuRegistration;
import dev.totem.core.api.v1.client.observer.ObserverRemoteCursor;
import dev.totem.core.api.v1.client.observer.ObserverScreenContext;
import dev.totem.core.api.v1.client.observer.ObserverScreenHandle;
import dev.totem.core.api.v1.client.observer.ObserverScreenProvider;
import dev.totem.core.api.v1.client.observer.ObserverScreenSnapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.EntityEquipment;
import net.minecraft.world.entity.player.Inventory;
import dev.totem.automata.network.CopperWrenchBindingsPayload;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import java.util.Optional;
import java.util.Map;

/** Automata-owned factory for the real Copper Golem screen. */
public final class AutomataObserverScreenProvider implements ObserverScreenProvider {
    public static final int PROTOCOL_VERSION = 1;

    @Override public String familyId() { return "automata_copper_golem"; }
    @Override public int protocolVersion() { return PROTOCOL_VERSION; }
    @Override public Set<String> variants() { return Set.of(""); }

    @Override public Optional<ObserverScreenSnapshot> capture(Screen candidate, long sequence) {
        if (!(candidate instanceof CopperGolemMenuScreen screen) || screen.totem$isObserverReadOnly()) {
            return Optional.empty();
        }
        return screen.observerCaptureSource().map(source -> {
            CopperWrenchBindingsPayload safe = redact(source);
            return new ObserverScreenSnapshot(familyId(), "", protocolVersion(), sequence, screen.getTitle(),
                    screen.getMenu().getItems(), new int[0], Map.of("golem_id", safe.golemId().toString()),
                    encode(safe));
        });
    }

    @Override
    public ObserverScreenHandle create(ObserverScreenContext context, ObserverScreenSnapshot snapshot) {
        if (!supports(snapshot)) throw new IllegalArgumentException("Incompatible Automata Observer snapshot");
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) throw new IllegalStateException("Observer player is unavailable");
        UUID golemId;
        try {
            golemId = UUID.fromString(snapshot.metadata().getOrDefault("golem_id", context.targetId().toString()));
        } catch (IllegalArgumentException invalid) {
            throw new IllegalArgumentException("Invalid observer golem identity", invalid);
        }
        Inventory detachedInventory = new Inventory(minecraft.player, new EntityEquipment());
        CopperGolemMenu menu = new CopperGolemMenu(CopperGolemMenuRegistration.TYPE, -1,
                detachedInventory, new CopperGolemMenuOpenData(golemId));
        CopperGolemMenuScreen screen = new CopperGolemMenuScreen(menu, detachedInventory,
                snapshot.title(), true, context.stopObserving());
        return new Handle(screen, menu, snapshot);
    }

    private final class Handle implements ObserverScreenHandle {
        private final CopperGolemMenuScreen screen;
        private final CopperGolemMenu menu;
        private long sequence = -1;
        private long cursorSequence = -1;

        private Handle(CopperGolemMenuScreen screen, CopperGolemMenu menu, ObserverScreenSnapshot initial) {
            this.screen = screen;
            this.menu = menu;
            applySnapshot(initial);
        }

        @Override public Screen screen() { return screen; }

        @Override public void applySnapshot(ObserverScreenSnapshot snapshot) {
            if (!AutomataObserverScreenProvider.this.supports(snapshot)
                    || snapshot.sequence() <= sequence) return;
            var items = new ArrayList<ItemStack>(menu.slots.size());
            var remoteSlots = snapshot.slots();
            for (int i = 0; i < menu.slots.size(); i++) {
                items.add(i < remoteSlots.size() ? remoteSlots.get(i).copy() : ItemStack.EMPTY);
            }
            menu.initializeContents((int) Math.min(Integer.MAX_VALUE, snapshot.sequence()), items, menu.getCarried());
            CopperWrenchBindingsPayload owner = decode(snapshot.ownerPayload());
            screen.acceptObserverSnapshot(owner);
            int[] data = snapshot.data();
            for (int i = 0; i < data.length; i++) menu.setData(i, data[i]);
            sequence = snapshot.sequence();
        }

        @Override public void applyCursor(ObserverRemoteCursor cursor) {
            if (cursor.sequence() <= cursorSequence) return;
            cursorSequence = cursor.sequence();
            menu.setCarried(cursor.carriedStack());
        }
    }

    private static byte[] encode(CopperWrenchBindingsPayload value) {
        var raw = Unpooled.buffer();
        FriendlyByteBuf buffer = new FriendlyByteBuf(raw);
        try {
            CopperWrenchBindingsPayload.CODEC.encode(buffer, value);
            byte[] bytes = new byte[buffer.readableBytes()];
            buffer.getBytes(buffer.readerIndex(), bytes);
            return bytes;
        } finally { buffer.release(); }
    }

    private static CopperWrenchBindingsPayload decode(byte[] bytes) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.wrappedBuffer(bytes));
        try {
            CopperWrenchBindingsPayload result = CopperWrenchBindingsPayload.CODEC.decode(buffer);
            if (buffer.readableBytes() != 0) throw new IllegalArgumentException("Trailing Automata Observer bytes");
            return result;
        } finally { buffer.release(); }
    }

    /** Security boundary: no credential, endpoint, model or editable prompt leaves the target. */
    private static CopperWrenchBindingsPayload redact(CopperWrenchBindingsPayload s) {
        var bindings = s.bindings().stream().map(b -> new CopperWrenchBindingsPayload.BindingEntry(
                b.dimension(), b.x(), b.y(), b.z(), b.blockId(), b.itemId(), b.loaded(), b.available(),
                b.llmEnabled(), "", b.llmCachedItemIds(), b.llmCachedTags(), b.llmAllowedItemIds(),
                b.llmDeniedItemIds(), b.llmAllowedTags(), b.llmDeniedTags())).toList();
        CopperWrenchBindingsPayload.BindingEntry source = s.sourceContainer();
        if (source != null) source = new CopperWrenchBindingsPayload.BindingEntry(
                source.dimension(), source.x(), source.y(), source.z(), source.blockId(), source.itemId(),
                source.loaded(), source.available(), source.llmEnabled(), "", source.llmCachedItemIds(),
                source.llmCachedTags(), source.llmAllowedItemIds(), source.llmDeniedItemIds(),
                source.llmAllowedTags(), source.llmDeniedTags());
        return new CopperWrenchBindingsPayload(s.golemId(), s.revision(), s.running(), s.mode(), s.activity(),
                s.fuelItemId(), s.fuelCount(), s.fuelTicks(), s.infiniteFuel(), s.gatheringToolItemId(),
                s.gatheringToolCount(), s.gatheringToolDamage(), s.gatheringToolMaxDamage(),
                s.gatheringStorageItemId(), s.gatheringStorageCount(), "", "", "", s.llmActiveCount(),
                source, s.gatheringArea(), s.gatheringManualTargets(), s.gatheringLlmEnabled(), "",
                s.gatheringLlmCachedBlockIds(), s.gatheringLlmCachedTags(), s.gatheringLlmAllowedBlockIds(),
                s.gatheringLlmDeniedBlockIds(), s.gatheringLlmAllowedTags(), s.gatheringLlmDeniedTags(), bindings);
    }
}
