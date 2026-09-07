package dev.totem.automata.copper;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CopperGolemDataMigrationTest {
    @Test
    void legacySchemaMigratesToCanonicalKeysAndCanonicalValuesWin() {
        CompoundTag tag = new CompoundTag();
        tag.putString("deadrecall_mode", CopperGolemMode.GATHERING.id());
        tag.putString(CopperGolemData.TAG_MODE, CopperGolemMode.SORTING.id());
        tag.putBoolean("deadrecall_transport_enabled", true);
        tag.putLong("deadrecall_gathering_scan_index", 41L);
        tag.putInt("deadrecall_gathering_storage_slot_0", 7);
        tag.putString("deadrecall_llm_api_url", "https://legacy.invalid/v1");
        tag.putBoolean("deadrecall_sorting_blocked", true);

        assertTrue(CopperGolemData.migrate(tag));

        assertEquals(CopperGolemMode.SORTING.id(), tag.getStringOr(CopperGolemData.TAG_MODE, ""));
        assertTrue(tag.getBooleanOr(CopperGolemData.TAG_TRANSPORT_ENABLED, false));
        assertEquals(41L, tag.getLongOr("totem_automata_gathering_scan_index", 0L));
        assertEquals(7, tag.getIntOr("totem_automata_gathering_storage_slot_0", 0));
        assertEquals("https://legacy.invalid/v1", tag.getStringOr("totem_automata_llm_api_url", ""));
        assertTrue(tag.getBooleanOr("totem_automata_sorting_blocked", false));
        assertEquals(CopperGolemData.DATA_VERSION, tag.getIntOr(CopperGolemData.TAG_DATA_VERSION, 0));
        assertFalse(tag.contains("deadrecall_mode"));
        assertFalse(tag.contains("deadrecall_transport_enabled"));
        assertFalse(tag.contains("deadrecall_gathering_scan_index"));
        assertFalse(tag.contains("deadrecall_gathering_storage_slot_0"));
        assertFalse(tag.contains("deadrecall_llm_api_url"));
        assertFalse(tag.contains("deadrecall_sorting_blocked"));
    }
}
