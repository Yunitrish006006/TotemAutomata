package dev.totem.automata.network;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;

/** Preserved server-configuration permission policy for Copper Golem LLM credentials. */
final class AutomataPayloadPermissions {
    private AutomataPayloadPermissions() {
    }

    static boolean canManageServerConfiguration(ServerPlayer player) {
        if (player.getAbilities().instabuild || player.isCreative()) {
            return true;
        }

        var server = player.level().getServer();
        if (server == null) {
            return false;
        }
        if (server.isSingleplayer()) {
            var owner = server.getSingleplayerProfile();
            if (owner != null && owner.id().equals(player.getGameProfile().id())) {
                return true;
            }
        }
        return server.getPlayerList().isOp(new NameAndId(player.getGameProfile()));
    }
}
