package dev.totem.automata.menu;

import dev.totem.automata.copper.CopperGolemData;
import dev.totem.automata.copper.PersistedCopperWrenchInteractionAuthority;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

import java.util.function.BiConsumer;

/**
 * Cutover-only menu opener for {@link PersistedCopperWrenchInteractionAuthority}.
 *
 * <p>The caller supplies the snapshot refresher so opening the menu and
 * sending the legacy clientbound payload remain one atomic cutover path.</p>
 */
public final class PersistedCopperGolemMenuOpener implements PersistedCopperWrenchInteractionAuthority.MenuOpener {
    private final BiConsumer<ServerPlayer, CopperGolem> refresher;
    public PersistedCopperGolemMenuOpener(BiConsumer<ServerPlayer, CopperGolem> refresher) { this.refresher = refresher; }
    @Override public void open(ServerPlayer player, CopperGolem golem) {
        var tag = CopperGolemData.readEntityTag(golem);
        if (CopperGolemData.migrate(tag)) CopperGolemData.writeEntityTag(golem, tag);
        CopperGolemMenuAuthority authority = new PersistedCopperGolemMenuAuthority(refresher);
        ExtendedMenuProvider<CopperGolemMenuOpenData> provider = new ExtendedMenuProvider<>() {
            @Override public CopperGolemMenuOpenData getScreenOpeningData(ServerPlayer ignored) { return new CopperGolemMenuOpenData(golem.getUUID()); }
            @Override public Component getDisplayName() { return Component.translatable("container.deadrecall.copper_wrench.bindings"); }
            @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player menuPlayer) {
                return new CopperGolemMenu(CopperGolemMenuRegistration.TYPE, id, inventory, menuPlayer, golem, authority);
            }
        };
        player.openMenu(provider).ifPresent(ignored -> refresher.accept(player, golem));
    }
}
