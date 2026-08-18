package dev.totem.automata.manual;

import dev.totem.core.api.v1.manual.TotemManualSection;
import dev.totem.core.api.v1.manual.TotemModuleManualSource;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;

import java.util.List;

/** Copper-golem setup guide recorded from any vanilla copper chest state. */
public final class AutomataManual {
    private static final TotemManualSection SECTION = new TotemManualSection(
            Identifier.fromNamespaceAndPath("totem", "automata/manual"),
            300,
            "book.deadrecall.automata_manual.title",
            List.of(
                    "book.deadrecall.automata_manual.page.1",
                    "book.deadrecall.automata_manual.page.2",
                    "book.deadrecall.automata_manual.page.3"
            )
    );

    private AutomataManual() {
    }

    public static void register() {
        TotemModuleManualSource.register(
                SECTION,
                Identifier.fromNamespaceAndPath("deadrecall", "automata_manual"),
                state -> state.is(BlockTags.COPPER_CHESTS)
        );
    }
}
