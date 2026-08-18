package dev.totem.automata.client;

/** Compact vanilla-inventory geometry for the Automata Copper Golem menu. */
public final class CopperGolemMenuPanelLayout {
    public static final int PREFERRED_WIDTH = 176;
    public static final int PREFERRED_HEIGHT = 222;
    public static final int MIN_WIDTH = 176;
    public static final int MIN_HEIGHT = 222;
    public static final int MARGIN = 6;
    public static final int PADDING = 12;

    private CopperGolemMenuPanelLayout() { }

    public static Bounds bounds(int windowWidth, int windowHeight) {
        int width = Math.max(MIN_WIDTH, Math.min(PREFERRED_WIDTH, windowWidth - MARGIN * 2));
        int height = Math.max(MIN_HEIGHT, Math.min(PREFERRED_HEIGHT, windowHeight - MARGIN * 2));
        return new Bounds((windowWidth - width) / 2, (windowHeight - height) / 2, width, height);
    }

    public record Bounds(int x, int y, int width, int height) { }
}
