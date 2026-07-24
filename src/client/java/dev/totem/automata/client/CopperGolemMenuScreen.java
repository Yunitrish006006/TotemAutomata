package dev.totem.automata.client;

import dev.totem.automata.menu.CopperGolemMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** In-progress Automata-owned Copper Golem renderer; registration remains cutover-only. */
public final class CopperGolemMenuScreen extends AbstractContainerScreen<CopperGolemMenu> {
    private final CopperGolemMenuScreenLifecycle lifecycle;
    private final CopperGolemMenuUiState ui = new CopperGolemMenuUiState();
    public CopperGolemMenuScreen(CopperGolemMenu menu, Inventory inventory, Component title) { super(menu, inventory, title, CopperGolemMenuPanelLayout.PREFERRED_WIDTH, CopperGolemMenuPanelLayout.PREFERRED_HEIGHT); lifecycle=new CopperGolemMenuScreenLifecycle(menu,inventory,title); }
    @Override protected void init(){super.init();lifecycle.open();var b=CopperGolemMenuPanelLayout.bounds(width,height);leftPos=b.x();topPos=b.y();addRenderableWidget(Button.builder(Component.translatable("gui.deadrecall.copper_wrench.operation"),x->lifecycle.session().toggleOperation()).bounds(b.x()+b.width()-86,b.y()+7,74,18).build());addRenderableWidget(Button.builder(Component.translatable("gui.deadrecall.copper_wrench.mode"),x->lifecycle.session().switchMode()).bounds(b.x()+b.width()-170,b.y()+7,78,18).build());addRenderableWidget(Button.builder(tabText(CopperGolemMenuUiState.Tab.BINDINGS),x->ui.tab(CopperGolemMenuUiState.Tab.BINDINGS)).bounds(b.x()+12,b.y()+26,70,18).build());addRenderableWidget(Button.builder(tabText(CopperGolemMenuUiState.Tab.LLM),x->ui.tab(CopperGolemMenuUiState.Tab.LLM)).bounds(b.x()+88,b.y()+26,70,18).build());addRenderableWidget(Button.builder(Component.translatable("gui.done"),x->onClose()).bounds(b.x()+b.width()/2-45,b.y()+b.height()-21,90,18).build());}
    @Override public void removed(){lifecycle.close();super.removed();}
    @Override public void extractBackground(GuiGraphicsExtractor g,int mouseX,int mouseY,float tick){g.fill(0,0,width,height,0xC0000000);}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mouseX,int mouseY,float tick){var b=CopperGolemMenuPanelLayout.bounds(width,height);g.fill(b.x(),b.y(),b.x()+b.width(),b.y()+b.height(),0xFF181818);g.outline(b.x(),b.y(),b.width(),b.height(),0xFF6A6A6A);g.text(font,title,b.x()+CopperGolemMenuPanelLayout.PADDING,b.y()+9,0xFFFFFFFF);super.extractRenderState(g,mouseX,mouseY,tick);}
    private Component tabText(CopperGolemMenuUiState.Tab tab){return Component.translatable(tab==CopperGolemMenuUiState.Tab.BINDINGS?"gui.deadrecall.copper_wrench.tab_bindings":"gui.deadrecall.copper_wrench.tab_llm").withStyle(tab==ui.tab()?net.minecraft.ChatFormatting.GREEN:net.minecraft.ChatFormatting.GRAY);}
}
