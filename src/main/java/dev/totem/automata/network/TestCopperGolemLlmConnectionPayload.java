package dev.totem.automata.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Serverbound LLM connection-test request. */
public record TestCopperGolemLlmConnectionPayload(String apiUrl, String apiKey, String model) implements CustomPacketPayload {
    public static final Type<TestCopperGolemLlmConnectionPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("deadrecall", "test_copper_golem_llm_connection"));
    public static final StreamCodec<FriendlyByteBuf, TestCopperGolemLlmConnectionPayload> CODEC = StreamCodec.of(
            (buf, p) -> { buf.writeUtf(p.apiUrl(), 2048); buf.writeUtf(p.apiKey(), 512); buf.writeUtf(p.model(), 256); },
            buf -> new TestCopperGolemLlmConnectionPayload(buf.readUtf(2048), buf.readUtf(512), buf.readUtf(256)));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
