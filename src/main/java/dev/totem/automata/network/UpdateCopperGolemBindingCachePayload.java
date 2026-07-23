package dev.totem.automata.network;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import java.util.UUID;
/** Serverbound per-binding LLM cache update. */
public record UpdateCopperGolemBindingCachePayload(UUID golemId,String dimension,int x,int y,int z,String value,boolean tag,boolean allowed,int revision) implements CustomPacketPayload {
 public static final Type<UpdateCopperGolemBindingCachePayload> TYPE=new Type<>(Identifier.fromNamespaceAndPath("deadrecall","update_copper_golem_binding_cache"));
 public static final StreamCodec<FriendlyByteBuf,UpdateCopperGolemBindingCachePayload> CODEC=StreamCodec.of((b,p)->{b.writeUUID(p.golemId());b.writeUtf(p.dimension(),128);b.writeInt(p.x());b.writeInt(p.y());b.writeInt(p.z());b.writeUtf(p.value(),256);b.writeBoolean(p.tag());b.writeBoolean(p.allowed());b.writeInt(p.revision());},b->new UpdateCopperGolemBindingCachePayload(b.readUUID(),b.readUtf(128),b.readInt(),b.readInt(),b.readInt(),b.readUtf(256),b.readBoolean(),b.readBoolean(),b.readInt()));
 @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
