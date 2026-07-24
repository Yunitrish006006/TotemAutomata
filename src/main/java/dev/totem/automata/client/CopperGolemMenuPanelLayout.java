package dev.totem.automata.client;

/** Responsive panel geometry preserved from the Copper Wrench menu UI. */
public final class CopperGolemMenuPanelLayout {
    public static final int PREFERRED_WIDTH=520, PREFERRED_HEIGHT=304, MIN_WIDTH=400, MIN_HEIGHT=236, MARGIN=6, PADDING=12;
    private CopperGolemMenuPanelLayout() { }
    public static Bounds bounds(int windowWidth, int windowHeight) {
        int width=Math.max(MIN_WIDTH,Math.min(PREFERRED_WIDTH,windowWidth-MARGIN*2)); int height=Math.max(MIN_HEIGHT,Math.min(PREFERRED_HEIGHT,windowHeight-MARGIN*2));
        return new Bounds((windowWidth-width)/2,(windowHeight-height)/2,width,height);
    }
    public record Bounds(int x,int y,int width,int height) { }
}
