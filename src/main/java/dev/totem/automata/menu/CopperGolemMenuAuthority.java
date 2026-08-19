package dev.totem.automata.menu;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Server-authoritative slot and access operations required by the Copper Golem menu. */
public interface CopperGolemMenuAuthority {
    boolean canUse(Player player, CopperGolem golem);
    boolean isFuel(ServerLevel level, ItemStack stack);
    boolean canEditGatheringSlots(CopperGolem golem);
    boolean isGatheringMode(CopperGolem golem);
    boolean isGatheringTool(ItemStack stack);
    int transportStorageMaxItemCount();
    ItemStack fuel(CopperGolem golem);
    ItemStack gatheringTool(CopperGolem golem);
    List<ItemStack> gatheringStorage(CopperGolem golem);
    void setFuel(CopperGolem golem, ItemStack stack);
    void setGatheringTool(CopperGolem golem, ItemStack stack);
    void setGatheringStorage(CopperGolem golem, List<ItemStack> stacks);
    void refresh(ServerPlayer viewer, CopperGolem golem);
}
