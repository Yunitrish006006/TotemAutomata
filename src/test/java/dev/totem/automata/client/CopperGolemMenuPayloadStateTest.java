package dev.totem.automata.client;

import dev.totem.automata.network.CopperWrenchBindingsPayload;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.assertSame;

class CopperGolemMenuPayloadStateTest {
    @Test void holdsOtherGolemPayloadUntilItsScreenOpens() {
        UUID id = UUID.randomUUID(); CopperWrenchBindingsPayload payload = new CopperWrenchBindingsPayload(id, 0, false, "sorting", "stopped", "minecraft:air", 0, 0, false, "minecraft:air", 0, 0, 0, "minecraft:air", 0, "", "", "", 0, null, null, List.of(), false, "", 0, 0, List.of(), List.of(), List.of(), List.of(), List.of());
        CopperGolemMenuPayloadState state = new CopperGolemMenuPayloadState(); AtomicReference<CopperWrenchBindingsPayload> received = new AtomicReference<>();
        state.receive(payload); state.open(id, received::set); assertSame(payload, received.get());
    }
}
