package dev.totem.automata.copper;

import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.golem.CopperGolem;

import java.util.Optional;
import java.util.UUID;

/** Persisted last operator used for gathering protection and break-event context. */
public final class GatheringOperator {
    private static final String KEY = "deadrecall_last_operator_player";
    private GatheringOperator() { }
    public static void remember(CopperGolem golem, ServerPlayer player) { CompoundTag tag=CopperGolemData.readEntityTag(golem);tag.store(KEY, UUIDUtil.CODEC, player.getUUID());CopperGolemData.writeEntityTag(golem,tag); }
    public static Optional<ServerPlayer> resolve(CopperGolem golem, ServerLevel level) { UUID id=CopperGolemData.readEntityTag(golem).read(KEY, UUIDUtil.CODEC).orElse(null); if(id==null)return Optional.empty(); ServerPlayer p=level.getServer().getPlayerList().getPlayer(id);return p!=null&&p.level()==level?Optional.of(p):Optional.empty(); }
}
