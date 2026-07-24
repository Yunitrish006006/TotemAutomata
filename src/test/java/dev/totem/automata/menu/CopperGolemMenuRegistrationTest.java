package dev.totem.automata.menu;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CopperGolemMenuRegistrationTest {
    @Test void preservesTheLegacyMenuIdentifierWithoutTouchingTheFrozenRegistry() {
        assertEquals("deadrecall:copper_golem", CopperGolemMenuIds.COPPER_GOLEM.toString());
    }
}
