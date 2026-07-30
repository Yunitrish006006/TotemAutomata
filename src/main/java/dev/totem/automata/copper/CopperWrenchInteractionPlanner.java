package dev.totem.automata.copper;

/**
 * Server-neutral decision table for Copper Wrench block and golem gestures.
 *
 * <p>The live callback adapter resolves entities, applies the returned intent,
 * and emits the matching message/particle effects.  This preserves the
 * legacy interaction rules while separating them from Fabric callbacks.</p>
 */
public final class CopperWrenchInteractionPlanner {
    private CopperWrenchInteractionPlanner() { }

    public static Intent leftClick(boolean hasSelectedGolem, Mode mode, Target target) {
        if (!hasSelectedGolem) return target.container() ? Intent.SELECT_GOLEM_FIRST : Intent.PASS;
        if (target.copperSource()) return Intent.REMOVE_SOURCE;
        if (mode == Mode.GATHERING) return target.container() ? Intent.REJECT_GATHERING_CONTAINER : Intent.TOGGLE_GATHERING_TARGET;
        return target.container() ? Intent.REMOVE_BINDING : Intent.PASS;
    }

    public static Intent useBlock(boolean hasSelectedGolem, Mode mode, boolean secondaryUse, Target target) {
        if (!hasSelectedGolem) return Intent.PASS;
        if (target.copperSource()) return Intent.SET_SOURCE;
        if (mode == Mode.GATHERING) return target.container()
                ? Intent.REJECT_GATHERING_CONTAINER
                : secondaryUse ? Intent.SET_GATHERING_CORNER_B : Intent.SET_GATHERING_CORNER_A;
        return target.container() ? Intent.ADD_BINDING : Intent.NEED_CONTAINER;
    }

    public static Intent useGolem(boolean holdsWrench, boolean ignoredSecondaryUse) {
        return holdsWrench ? Intent.SELECT_GOLEM_AND_OPEN_MENU : Intent.PASS;
    }

    public enum Mode { SORTING, GATHERING }
    public record Target(boolean container, boolean copperSource) { }
    public enum Intent {
        PASS, SELECT_GOLEM_FIRST, REMOVE_SOURCE, TOGGLE_GATHERING_TARGET, REMOVE_BINDING,
        REJECT_GATHERING_CONTAINER, SET_SOURCE, SET_GATHERING_CORNER_A, SET_GATHERING_CORNER_B,
        ADD_BINDING, NEED_CONTAINER, SELECT_GOLEM_AND_OPEN_MENU
    }
}
