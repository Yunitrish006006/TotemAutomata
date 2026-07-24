package dev.totem.automata.copper;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CopperWrenchInteractionPlannerTest {
    private static final CopperWrenchInteractionPlanner.Target CONTAINER = new CopperWrenchInteractionPlanner.Target(true, false);
    private static final CopperWrenchInteractionPlanner.Target SOURCE = new CopperWrenchInteractionPlanner.Target(true, true);
    private static final CopperWrenchInteractionPlanner.Target BLOCK = new CopperWrenchInteractionPlanner.Target(false, false);

    @Test void preservesSortingSourceAndBindingGestures() {
        assertEquals(CopperWrenchInteractionPlanner.Intent.SET_SOURCE,
                CopperWrenchInteractionPlanner.useBlock(true, CopperWrenchInteractionPlanner.Mode.SORTING, false, SOURCE));
        assertEquals(CopperWrenchInteractionPlanner.Intent.ADD_BINDING,
                CopperWrenchInteractionPlanner.useBlock(true, CopperWrenchInteractionPlanner.Mode.SORTING, false, CONTAINER));
        assertEquals(CopperWrenchInteractionPlanner.Intent.REMOVE_BINDING,
                CopperWrenchInteractionPlanner.leftClick(true, CopperWrenchInteractionPlanner.Mode.SORTING, CONTAINER));
        assertEquals(CopperWrenchInteractionPlanner.Intent.NEED_CONTAINER,
                CopperWrenchInteractionPlanner.useBlock(true, CopperWrenchInteractionPlanner.Mode.SORTING, false, BLOCK));
    }

    @Test void preservesGatheringCornerAndTargetRules() {
        assertEquals(CopperWrenchInteractionPlanner.Intent.SET_GATHERING_CORNER_A,
                CopperWrenchInteractionPlanner.useBlock(true, CopperWrenchInteractionPlanner.Mode.GATHERING, false, BLOCK));
        assertEquals(CopperWrenchInteractionPlanner.Intent.SET_GATHERING_CORNER_B,
                CopperWrenchInteractionPlanner.useBlock(true, CopperWrenchInteractionPlanner.Mode.GATHERING, true, BLOCK));
        assertEquals(CopperWrenchInteractionPlanner.Intent.TOGGLE_GATHERING_TARGET,
                CopperWrenchInteractionPlanner.leftClick(true, CopperWrenchInteractionPlanner.Mode.GATHERING, BLOCK));
        assertEquals(CopperWrenchInteractionPlanner.Intent.REJECT_GATHERING_CONTAINER,
                CopperWrenchInteractionPlanner.useBlock(true, CopperWrenchInteractionPlanner.Mode.GATHERING, false, CONTAINER));
    }

    @Test void requiresShiftWrenchUseToSelectAndOpenGolemMenu() {
        assertEquals(CopperWrenchInteractionPlanner.Intent.PASS, CopperWrenchInteractionPlanner.useGolem(true, false));
        assertEquals(CopperWrenchInteractionPlanner.Intent.SELECT_GOLEM_AND_OPEN_MENU, CopperWrenchInteractionPlanner.useGolem(true, true));
    }
}
