package dev.totem.automata.advancement;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

/** Preserved Automata advancement trigger identifiers. */
public final class AutomataCriteria {
    public static final SimplePlayerCriterionTrigger FIRST_COPPER_GOLEM_BINDING = Registry.register(
            BuiltInRegistries.TRIGGER_TYPES,
            AutomataCriterionIds.FIRST_COPPER_GOLEM_BINDING,
            new SimplePlayerCriterionTrigger());

    private AutomataCriteria() { }
    public static void register() { /* class loading registers criteria at cutover */ }
}
