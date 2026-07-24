package dev.totem.automata.copper;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.golem.CopperGolem;

import java.util.UUID;

/** Server-side Wrench authorization used by every future menu and payload mutation. */
public final class CopperGolemWrenchAccess {
    private static final double MANAGEMENT_DISTANCE_SQR = 64.0D * 64.0D;

    private CopperGolemWrenchAccess() { }

    public static Result validate(ServerPlayer player, UUID golemId, int clientRevision) {
        if (golemId == null) return Result.rejected(Reason.UNAVAILABLE);
        SelectionResult selected = resolveSelectedGolem(player, golemId);
        if (!selected.available()) return Result.rejected(switch (selected.reason()) {
            case OTHER_DIMENSION -> Reason.OTHER_DIMENSION;
            default -> Reason.UNAVAILABLE;
        });
        CopperGolem golem = selected.golem();
        if (player.distanceToSqr(golem) > MANAGEMENT_DISTANCE_SQR) return Result.rejected(Reason.TOO_FAR);
        if (!holdsBoundWrench(player, golemId)) return Result.rejected(Reason.INVALID_WRENCH);

        CompoundTag tag = CopperGolemData.readEntityTag(golem);
        if (CopperGolemData.migrate(tag)) CopperGolemData.writeEntityTag(golem, tag);
        int revision = tag.getIntOr(CopperGolemData.TAG_REVISION, 0);
        if (clientRevision != revision) return Result.rejected(Reason.STALE_REVISION);
        return Result.allowed(golem, revision);
    }

    /** Resolves a selected golem without applying the menu-only distance or revision checks. */
    public static SelectionResult resolveSelectedGolem(ServerPlayer player, net.minecraft.world.item.ItemStack wrench) {
        return resolveSelectedGolem(player, CopperWrenchSelection.selectedGolem(wrench));
    }

    /** Resolves an explicit selection for Wrench block/entity interactions. */
    public static SelectionResult resolveSelectedGolem(ServerPlayer player, UUID golemId) {
        if (golemId == null) return SelectionResult.rejected(SelectionReason.NO_SELECTION);
        Entity entity = player.level().getEntityInAnyDimension(golemId);
        if (!(entity instanceof CopperGolem golem) || golem.isRemoved() || !golem.isAlive())
            return SelectionResult.rejected(SelectionReason.UNAVAILABLE);
        if (!golem.level().dimension().equals(player.level().dimension()))
            return SelectionResult.rejected(SelectionReason.OTHER_DIMENSION);
        return SelectionResult.available(golem);
    }

    public static boolean holdsBoundWrench(ServerPlayer player, UUID golemId) {
        return golemId != null && (golemId.equals(CopperWrenchSelection.selectedGolem(player.getMainHandItem()))
                || golemId.equals(CopperWrenchSelection.selectedGolem(player.getOffhandItem())));
    }

    public enum Reason { ALLOWED, UNAVAILABLE, OTHER_DIMENSION, TOO_FAR, INVALID_WRENCH, STALE_REVISION }
    public enum SelectionReason { AVAILABLE, NO_SELECTION, UNAVAILABLE, OTHER_DIMENSION }

    public record Result(CopperGolem golem, int revision, Reason reason) {
        static Result allowed(CopperGolem golem, int revision) { return new Result(golem, revision, Reason.ALLOWED); }
        static Result rejected(Reason reason) { return new Result(null, -1, reason); }
        public boolean allowed() { return reason == Reason.ALLOWED; }
    }
    public record SelectionResult(CopperGolem golem, SelectionReason reason) {
        static SelectionResult available(CopperGolem golem) { return new SelectionResult(golem, SelectionReason.AVAILABLE); }
        static SelectionResult rejected(SelectionReason reason) { return new SelectionResult(null, reason); }
        public boolean available() { return reason == SelectionReason.AVAILABLE; }
    }
}
