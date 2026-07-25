package dev.totem.automata.mixin.client;

import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Cutover-only slot placement accessor used by the migrated Wrench menu screen. */
@Mixin(Slot.class)
public interface SlotAccessor {
    @Mutable
    @Accessor("x")
    void totemAutomata$setX(int x);

    @Mutable
    @Accessor("y")
    void totemAutomata$setY(int y);
}
