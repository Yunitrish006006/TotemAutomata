package dev.totem.automata.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.UUID;

/** Serverbound revision-checked LLM configuration update. */
public record SaveCopperGolemLlmConfigPayload(UUID golemId, String apiUrl, String apiKey, String model, int revision)
        implements CustomPacketPayload {
    public static final Type<SaveCopperGolemLlmConfigPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("deadrecall", "save_copper_golem_llm_config"));
    public static final StreamCodec<FriendlyByteBuf, SaveCopperGolemLlmConfigPayload> CODEC = StreamCodec.of(
            (buf, p) -> { buf.writeUUID(p.golemId()); buf.writeUtf(p.apiUrl(), 2048); buf.writeUtf(p.apiKey(), 512); buf.writeUtf(p.model(), 256); buf.writeInt(p.revision()); },
            buf -> new SaveCopperGolemLlmConfigPayload(buf.readUUID(), buf.readUtf(2048), buf.readUtf(512), buf.readUtf(256), buf.readInt()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
