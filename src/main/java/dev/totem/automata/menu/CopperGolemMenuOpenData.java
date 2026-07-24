package dev.totem.automata.menu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.UUID;

/** Preserved extended-menu opening payload. */
public record CopperGolemMenuOpenData(UUID golemId) {
    public static final StreamCodec<RegistryFriendlyByteBuf, CopperGolemMenuOpenData> STREAM_CODEC = StreamCodec.of(
            (buf, data) -> buf.writeUUID(data.golemId()), buf -> new CopperGolemMenuOpenData(buf.readUUID()));
}
