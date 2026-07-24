package dev.totem.automata.advancement;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AutomataCriteriaTest {
    @Test void preservesTheFirstBindingTriggerIdentifier() {
        assertEquals("deadrecall:first_copper_golem_binding",
                AutomataCriterionIds.FIRST_COPPER_GOLEM_BINDING.toString());
    }
}
