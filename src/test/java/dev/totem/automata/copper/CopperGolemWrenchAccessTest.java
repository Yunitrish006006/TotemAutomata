package dev.totem.automata.copper;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class CopperGolemWrenchAccessTest {
    @Test void rejectedResultsNeverExposeAnEntityOrRevision() {
        for (CopperGolemWrenchAccess.Reason reason : CopperGolemWrenchAccess.Reason.values()) {
            if (reason == CopperGolemWrenchAccess.Reason.ALLOWED) continue;
            CopperGolemWrenchAccess.Result result = CopperGolemWrenchAccess.Result.rejected(reason);
            assertFalse(result.allowed()); assertNull(result.golem()); assertSame(reason, result.reason());
        }
    }
}
