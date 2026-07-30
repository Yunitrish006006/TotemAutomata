package dev.totem.automata.client;

/** Responsive panel geometry preserved from the Copper Wrench menu UI. */
public final class CopperGolemMenuPanelLayout {
    public static final int PREFERRED_WIDTH = 520;
    public static final int PREFERRED_HEIGHT = 304;
    public static final int MIN_WIDTH = 400;
    public static final int MIN_HEIGHT = 236;
    public static final int MARGIN = 6;
    public static final int PADDING = 12;

    private static final int BINDING_LIST_TOP = 108;
    private static final int BINDING_EDITOR_BOTTOM_SPACE = 86;
    private static final int BINDING_CARD_STRIDE = 28;

    private CopperGolemMenuPanelLayout() { }

    public static Bounds bounds(int windowWidth, int windowHeight) {
        int width = Math.max(MIN_WIDTH, Math.min(PREFERRED_WIDTH, windowWidth - MARGIN * 2));
        int height = Math.max(MIN_HEIGHT, Math.min(PREFERRED_HEIGHT, windowHeight - MARGIN * 2));
        return new Bounds((windowWidth - width) / 2, (windowHeight - height) / 2, width, height);
    }

    public static int bindingListY(Bounds bounds) {
        return bounds.y() + BINDING_LIST_TOP;
    }

    public static int bindingControlsY(Bounds bounds) {
        return bounds.y() + bounds.height() - BINDING_EDITOR_BOTTOM_SPACE;
    }

    public static int bindingManualRulesY(Bounds bounds) {
        return bindingControlsY(bounds) - 12;
    }

    public static int visibleBindingRows(Bounds bounds) {
        return Math.max(1, Math.min(3,
                (bindingManualRulesY(bounds) - bindingListY(bounds) - 2) / BINDING_CARD_STRIDE));
    }

    public record Bounds(int x, int y, int width, int height) { }
}
