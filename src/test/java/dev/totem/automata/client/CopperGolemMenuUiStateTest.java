package dev.totem.automata.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CopperGolemMenuUiStateTest {
    @Test
    void clampsSelectionAndScroll() {
        var state = new CopperGolemMenuUiState();
        state.select(9, 2);
        assertEquals(1, state.selected());
        state.select(0, 0);
        assertEquals(-1, state.selected());
        state.scroll(20, 5);
        assertEquals(5, state.scroll());
    }

    @Test
    void keepsAllowedAndDeniedFilterScrollIndependentAndBounded() {
        var state = new CopperGolemMenuUiState();
        state.filterScroll(true, 4, 12);
        state.filterScroll(false, 8, 8);

        assertEquals(4, state.filterScroll(true));
        assertEquals(8, state.filterScroll(false));

        state.filterScroll(true, 20, 12);
        state.filterScroll(false, -4, 8);
        assertEquals(12, state.filterScroll(true));
        assertEquals(0, state.filterScroll(false));

        state.resetFilterScroll();
        assertEquals(0, state.filterScroll(true));
        assertEquals(0, state.filterScroll(false));
    }
}
