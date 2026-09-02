package dev.totem.automata.client;

/** Renderer-independent tab, selection, and scroll state for the migrated menu. */
public final class CopperGolemMenuUiState {
    private Tab tab=Tab.BINDINGS; private int selected=-1, scroll, allowedFilterScroll, deniedFilterScroll;
    public Tab tab(){return tab;} public void tab(Tab value){tab=value==null?Tab.BINDINGS:value;}
    public int selected(){return selected;} public void select(int index,int count){selected=index<0||index>=count?(count==0?-1:Math.min(Math.max(0,index),count-1)):index;}
    public int scroll(){return scroll;} public void scroll(int value,int max){scroll=Math.max(0,Math.min(value,Math.max(0,max)));}
    public int filterScroll(boolean allowed){return allowed?allowedFilterScroll:deniedFilterScroll;}
    public void filterScroll(boolean allowed,int value,int max){int clamped=Math.max(0,Math.min(value,Math.max(0,max)));if(allowed)allowedFilterScroll=clamped;else deniedFilterScroll=clamped;}
    public void resetFilterScroll(){allowedFilterScroll=0;deniedFilterScroll=0;}
    public enum Tab { BINDINGS, LLM }
}
