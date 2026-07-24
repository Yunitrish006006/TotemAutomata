package dev.totem.automata.client;
import org.junit.jupiter.api.Test;import static org.junit.jupiter.api.Assertions.*;
class CopperGolemMenuUiStateTest{@Test void clampsSelectionAndScroll(){var state=new CopperGolemMenuUiState();state.select(9,2);assertEquals(1,state.selected());state.select(0,0);assertEquals(-1,state.selected());state.scroll(20,5);assertEquals(5,state.scroll());}}
