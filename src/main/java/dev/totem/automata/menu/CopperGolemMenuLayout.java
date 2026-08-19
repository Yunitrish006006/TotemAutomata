package dev.totem.automata.menu;

/** Stable Copper Golem menu slot indices and layout coordinates shared by server and client. */
public final class CopperGolemMenuLayout {
    public static final int SLOT_FUEL = 0;
    public static final int SLOT_GATHERING_TOOL = 1;
    public static final int SLOT_GATHERING_STORAGE_START = 2;
    /** Compatibility alias for client code that refers to the first carried-item slot. */
    public static final int SLOT_GATHERING_STORAGE = SLOT_GATHERING_STORAGE_START;
    public static final int GATHERING_STORAGE_SLOT_COUNT = 16;
    public static final int GOLEM_SLOT_COUNT = SLOT_GATHERING_STORAGE_START + GATHERING_STORAGE_SLOT_COUNT;

    public static final int FUEL_SLOT_X = 150, FUEL_SLOT_Y = 26;
    public static final int GATHERING_TOOL_SLOT_X = 96, GATHERING_TOOL_SLOT_Y = 26;
    public static final int PLAYER_INVENTORY_X = 342, PLAYER_INVENTORY_Y = 146, PLAYER_HOTBAR_Y = 204;

    /* The first three positions are retained by the current client layout. The remaining carried slots avoid the fuel slot. */
    private static final int[] STORAGE_X = {
            119, 137, 155,
            83, 101, 137, 155,
            83, 101, 137, 155,
            83, 101, 119, 137, 155
    };
    private static final int[] STORAGE_Y = {
            42, 42, 42,
            62, 62, 62, 62,
            80, 80, 80, 80,
            98, 98, 98, 98, 98
    };

    public static int gatheringStorageSlotIndex(int storageIndex) {
        return SLOT_GATHERING_STORAGE_START + storageIndex;
    }

    public static int gatheringStorageX(int storageIndex) {
        return STORAGE_X[storageIndex];
    }

    public static int gatheringStorageY(int storageIndex) {
        return STORAGE_Y[storageIndex];
    }

    private CopperGolemMenuLayout() { }
}
