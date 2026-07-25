package dev.totem.automata.mixin;

import dev.totem.automata.copper.CopperGolemSortingAuthority;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.TransportItemsBetweenContainers;
import net.minecraft.world.entity.ai.behavior.TransportItemsBetweenContainers.TransportItemTarget;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/**
 * Preserved sorting transport behavior, kept inactive until the atomic
 * Automata cutover registers this package's mixin configuration.
 */
@Mixin(TransportItemsBetweenContainers.class)
public abstract class TransportItemsBetweenContainersMixin {
    @Shadow private TransportItemTarget target;
    @Shadow protected abstract void clearMemoriesAfterMatchingTargetFound(PathfinderMob mob);
    @Shadow protected abstract void stopTargetingCurrentTarget(PathfinderMob mob);

    @Inject(method = "checkExtraStartConditions(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/PathfinderMob;)Z", at = @At("HEAD"), cancellable = true)
    private void totemAutomata$requireBindingBeforeTransport(ServerLevel level, PathfinderMob mob, CallbackInfoReturnable<Boolean> cir) {
        if (mob instanceof CopperGolem golem
                && (!CopperGolemSortingAuthority.sortingMode(golem)
                || !CopperGolemSortingAuthority.hasBinding(golem)
                || !CopperGolemSortingAuthority.transportEnabled(golem)
                || CopperGolemSortingAuthority.sortingBlocked(golem)
                || (golem.getMainHandItem().isEmpty()
                && (!CopperGolemSortingAuthority.hasSource(golem)
                || !CopperGolemSortingAuthority.hasFuel(golem, level))))) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "getTransportTarget", at = @At("HEAD"), cancellable = true)
    private void totemAutomata$useBoundCopperGolemTarget(ServerLevel level, PathfinderMob mob, CallbackInfoReturnable<Optional<TransportItemTarget>> cir) {
        if (!(mob instanceof CopperGolem golem) || !CopperGolemSortingAuthority.sortingMode(golem) || mob.getMainHandItem().isEmpty()) return;
        if (!CopperGolemSortingAuthority.hasBinding(golem)) return;
        if (!CopperGolemSortingAuthority.transportEnabled(golem)) { cir.setReturnValue(Optional.empty()); return; }
        Optional<TransportItemTarget> target = CopperGolemSortingAuthority.nextDestination(golem, level, mob.getMainHandItem());
        if (target.isPresent()) { cir.setReturnValue(target); return; }
        if (!CopperGolemSortingAuthority.returnCarried(golem, level)) cir.setReturnValue(Optional.empty());
    }

    @Inject(method = "isWantedBlock", at = @At("HEAD"), cancellable = true)
    private void totemAutomata$acceptBoundCopperGolemTargetState(PathfinderMob mob, BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (mob instanceof CopperGolem golem && !mob.getMainHandItem().isEmpty()
                && CopperGolemSortingAuthority.sortingMode(golem)
                && CopperGolemSortingAuthority.hasBinding(golem)
                && CopperGolemSortingAuthority.transportEnabled(golem)) cir.setReturnValue(true);
    }

    @Inject(method = "pickUpItems", at = @At("HEAD"), cancellable = true)
    private void totemAutomata$pickUpSortableItem(PathfinderMob mob, Container container, CallbackInfo ci) {
        if (!(mob instanceof CopperGolem golem) || !CopperGolemSortingAuthority.sortingMode(golem)
                || !CopperGolemSortingAuthority.hasSource(golem) || !CopperGolemSortingAuthority.hasBinding(golem)
                || !CopperGolemSortingAuthority.transportEnabled(golem) || !(mob.level() instanceof ServerLevel level) || target == null) return;
        if (!CopperGolemSortingAuthority.sourceAt(golem, level, target.pos()) || !CopperGolemSortingAuthority.hasFuel(golem, level)) {
            stopTargetingCurrentTarget(mob); ci.cancel(); return;
        }
        ItemStack picked = CopperGolemSortingAuthority.pickUp(golem, level, container, target.pos());
        if (picked.isEmpty()) { stopTargetingCurrentTarget(mob); ci.cancel(); return; }
        mob.setItemSlot(EquipmentSlot.MAINHAND, picked);
        mob.setGuaranteedDrop(EquipmentSlot.MAINHAND);
        container.setChanged();
        clearMemoriesAfterMatchingTargetFound(mob);
        ci.cancel();
    }

    @Inject(method = "putDownItem", at = @At("HEAD"), cancellable = true)
    private void totemAutomata$putDownItemIntoDestination(PathfinderMob mob, Container container, CallbackInfo ci) {
        if (!(mob instanceof CopperGolem golem) || !CopperGolemSortingAuthority.sortingMode(golem)
                || !CopperGolemSortingAuthority.hasBinding(golem) || !CopperGolemSortingAuthority.transportEnabled(golem)
                || !(mob.level() instanceof ServerLevel level) || target == null) return;
        Optional<ItemStack> remaining = CopperGolemSortingAuthority.deposit(golem, level, target.pos(), container);
        if (remaining.isEmpty()) return;
        ItemStack remainingStack = remaining.get();
        mob.setItemSlot(EquipmentSlot.MAINHAND, remainingStack);
        if (remainingStack.isEmpty()) {
            clearMemoriesAfterMatchingTargetFound(mob);
            CopperGolemSortingAuthority.clearRememberedSource(golem);
        } else stopTargetingCurrentTarget(mob);
        ci.cancel();
    }

    @Inject(method = "putDownItem", at = @At("TAIL"))
    private void totemAutomata$clearSourceAfterPuttingDownItem(PathfinderMob mob, Container container, CallbackInfo ci) {
        if (mob instanceof CopperGolem golem && CopperGolemSortingAuthority.sortingMode(golem)
                && mob.getItemInHand(InteractionHand.MAIN_HAND).isEmpty()) CopperGolemSortingAuthority.clearRememberedSource(golem);
    }
}
