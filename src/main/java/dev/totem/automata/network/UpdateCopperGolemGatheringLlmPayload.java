package dev.totem.automata.network;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import java.util.UUID;
/** Serverbound gathering LLM configuration update. */
public record UpdateCopperGolemGatheringLlmPayload(UUID golemId, boolean enabled, String prompt, int revision) implements CustomPacketPayload {
 public static final Type<UpdateCopperGolemGatheringLlmPayload> TYPE=new Type<>(Identifier.fromNamespaceAndPath("deadrecall","update_copper_golem_gathering_llm"));
 public static final StreamCodec<FriendlyByteBuf,UpdateCopperGolemGatheringLlmPayload> CODEC=StreamCodec.of((b,p)->{b.writeUUID(p.golemId());b.writeBoolean(p.enabled());b.writeUtf(p.prompt(),2048);b.writeInt(p.revision());},b->new UpdateCopperGolemGatheringLlmPayload(b.readUUID(),b.readBoolean(),b.readUtf(2048),b.readInt()));
 @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
