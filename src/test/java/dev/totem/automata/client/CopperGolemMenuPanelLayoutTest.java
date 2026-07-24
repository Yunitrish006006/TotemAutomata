package dev.totem.automata.client;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CopperGolemMenuPanelLayoutTest {
 @Test void centersPreferredAndEnforcesLegacyMinimum(){var b=CopperGolemMenuPanelLayout.bounds(800,600);assertEquals(520,b.width());assertEquals(304,b.height());var small=CopperGolemMenuPanelLayout.bounds(410,245);assertEquals(400,small.width());assertEquals(236,small.height());}
}
