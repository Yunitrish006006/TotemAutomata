package dev.totem.automata.mixin;

import dev.totem.automata.copper.CopperGolemLifecycle;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Cutover-only destruction cleanup; listed in the external mixin config only at activation. */
@Mixin(Entity.class)
public abstract class CopperGolemEntityMixin {
    @Inject(method = "remove", at = @At("HEAD"))
    private void totemAutomata$dropCopperGolemInventoryOnDestroy(Entity.RemovalReason reason, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (reason.shouldDestroy() && !self.level().isClientSide() && self instanceof CopperGolem golem) {
            CopperGolemLifecycle.dropGatheringInventory(golem);
        }
    }
}
