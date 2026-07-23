package dev.totem.automata.copper;

import java.util.Locale;

/** Stable persisted Copper Golem operation modes. */
public enum CopperGolemMode {
    SORTING("sorting"),
    GATHERING("gathering");

    private final String id;

    CopperGolemMode(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public CopperGolemMode next() {
        return this == SORTING ? GATHERING : SORTING;
    }

    public static CopperGolemMode fromId(String id) {
        if (id == null || id.isBlank()) {
            return SORTING;
        }
        String normalized = id.toLowerCase(Locale.ROOT);
        for (CopperGolemMode mode : values()) {
            if (mode.id.equals(normalized)) {
                return mode;
            }
        }
        return SORTING;
    }
}
