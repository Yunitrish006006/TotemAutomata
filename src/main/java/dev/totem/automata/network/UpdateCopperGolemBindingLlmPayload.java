package dev.totem.automata.network;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import java.util.UUID;
/** Serverbound binding LLM configuration update. */
public record UpdateCopperGolemBindingLlmPayload(UUID golemId, String dimension, int x, int y, int z, boolean enabled, String prompt, int revision) implements CustomPacketPayload {
 public static final Type<UpdateCopperGolemBindingLlmPayload> TYPE=new Type<>(Identifier.fromNamespaceAndPath("deadrecall","update_copper_golem_binding_llm"));
 public static final StreamCodec<FriendlyByteBuf,UpdateCopperGolemBindingLlmPayload> CODEC=StreamCodec.of((b,p)->{b.writeUUID(p.golemId());b.writeUtf(p.dimension(),128);b.writeInt(p.x());b.writeInt(p.y());b.writeInt(p.z());b.writeBoolean(p.enabled());b.writeUtf(p.prompt(),2048);b.writeInt(p.revision());},b->new UpdateCopperGolemBindingLlmPayload(b.readUUID(),b.readUtf(128),b.readInt(),b.readInt(),b.readInt(),b.readBoolean(),b.readUtf(2048),b.readInt()));
 @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
