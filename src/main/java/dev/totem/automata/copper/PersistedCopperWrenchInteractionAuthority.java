package dev.totem.automata.copper;

import dev.totem.core.api.v1.migration.LegacyItemMigrationRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * Automata's server-side Wrench gesture executor.
 *
 * <p>Registration and menu opening are deliberately injected; this class is
 * safe to build and test during the additive phase without taking ownership
 * of DeadRecall's active callbacks.</p>
 */
public final class PersistedCopperWrenchInteractionAuthority implements CopperWrenchInteractionAuthority {
    private static final float REPAIR_AMOUNT = 4.0F;
    private final MenuOpener menuOpener;
    private final BindingCriterion bindingCriterion;
    private final CopperWrenchInteractionDebounce debounce = new CopperWrenchInteractionDebounce();
    public PersistedCopperWrenchInteractionAuthority(MenuOpener menuOpener) { this(menuOpener, player -> { }); }
    public PersistedCopperWrenchInteractionAuthority(MenuOpener menuOpener, BindingCriterion bindingCriterion) { this.menuOpener = menuOpener; this.bindingCriterion = bindingCriterion; }

    @Override public InteractionResult attackBlock(Player player, Level level, InteractionHand hand, BlockPos pos) {
        ItemStack wrench = player.getItemInHand(hand);
        if (player.isSpectator() || !CopperWrenchSelection.isCopperWrench(wrench)) return InteractionResult.PASS;
        wrench = migrateHeldWrench(player, level, hand, wrench);
        Target target = target(level, pos); if (CopperWrenchSelection.selectedGolem(wrench) == null) {
            if (target.container && !level.isClientSide()) CopperWrenchFeedback.send(player, CopperWrenchInteractionPlanner.Intent.SELECT_GOLEM_FIRST, false, "");
            return target.container ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        CopperGolem golem = resolve(player, wrench); if (golem == null) return InteractionResult.SUCCESS;
        if (player instanceof ServerPlayer server) GatheringOperator.remember(golem, server);
        CopperWrenchInteractionPlanner.Intent intent = CopperWrenchInteractionPlanner.leftClick(true, mode(golem), target.plannerTarget());
        if (intent == CopperWrenchInteractionPlanner.Intent.TOGGLE_GATHERING_TARGET
                && debounce.isGatheringTargetDuplicate(player.getUUID(), golem.getUUID(), level.dimension(), pos,
                BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).toString(), level.getGameTime())) return InteractionResult.SUCCESS;
        return execute(player, golem, level, pos, intent);
    }

    @Override public InteractionResult useBlock(Player player, Level level, InteractionHand hand, BlockPos pos) {
        ItemStack wrench = player.getItemInHand(hand);
        if (!CopperWrenchSelection.isCopperWrench(wrench)) return InteractionResult.PASS;
        wrench = migrateHeldWrench(player, level, hand, wrench);
        if (debounce.consumeEntityToBlockSuppression(player.getUUID(), hand, level.isClientSide(), level.getGameTime())) return InteractionResult.SUCCESS;
        if (CopperWrenchSelection.selectedGolem(wrench) == null) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        CopperGolem golem = resolve(player, wrench); if (golem == null) return InteractionResult.SUCCESS;
        if (player instanceof ServerPlayer server) GatheringOperator.remember(golem, server);
        return execute(player, golem, level, pos, CopperWrenchInteractionPlanner.useBlock(true, mode(golem), player.isShiftKeyDown(), target(level, pos).plannerTarget()));
    }

    @Override public InteractionResult useEntity(Player player, Level level, InteractionHand hand, Entity entity) {
        ItemStack wrench = player.getItemInHand(hand);
        if (!(entity instanceof CopperGolem golem)) return InteractionResult.PASS;
        if (wrench.is(Items.COPPER_INGOT)) return repair(player, level, wrench, golem);
        if (!CopperWrenchSelection.isCopperWrench(wrench)) return InteractionResult.PASS;
        wrench = migrateHeldWrench(player, level, hand, wrench);
        // A Wrench right-click both selects the Golem for target configuration
        // and opens its menu. Shift is not required.
        debounce.recordEntityUse(player.getUUID(), hand, level.isClientSide(), level.getGameTime());
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        CopperWrenchSelection.select(wrench, golem.getUUID());
        if (player instanceof ServerPlayer server) { GatheringOperator.remember(golem, server); menuOpener.open(server, golem); }
        return InteractionResult.SUCCESS;
    }

    private static InteractionResult repair(Player player, Level level, ItemStack ingot, CopperGolem golem) {
        if (!golem.isAlive() || golem.getHealth() >= golem.getMaxHealth()) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        float before = golem.getHealth(); golem.heal(REPAIR_AMOUNT);
        if (golem.getHealth() <= before) return InteractionResult.PASS;
        if (!player.getAbilities().instabuild) ingot.shrink(1);
        if (level instanceof ServerLevel server) server.sendParticles(ParticleTypes.WAX_ON, golem.getX(), golem.getY() + golem.getBbHeight() * .65D, golem.getZ(), 8, .25D, .35D, .25D, .02D);
        return InteractionResult.SUCCESS;
    }

    private static ItemStack migrateHeldWrench(
            Player player,
            Level level,
            InteractionHand hand,
            ItemStack wrench
    ) {
        if (level.isClientSide()) return wrench;
        ItemStack migrated = LegacyItemMigrationRegistry.migrate(wrench);
        if (migrated != wrench) player.setItemInHand(hand, migrated);
        return migrated;
    }

    private CopperGolem resolve(Player player, ItemStack wrench) {
        if (!(player instanceof ServerPlayer server)) return null;
        CopperGolemWrenchAccess.SelectionResult resolved = CopperGolemWrenchAccess.resolveSelectedGolem(server, wrench);
        if (!resolved.available()) { CopperWrenchSelection.clear(wrench); return null; }
        return resolved.golem();
    }
    private static CopperWrenchInteractionPlanner.Mode mode(CopperGolem golem) { return CopperGolemData.mode(CopperGolemData.readEntityTag(golem)) == CopperGolemMode.GATHERING ? CopperWrenchInteractionPlanner.Mode.GATHERING : CopperWrenchInteractionPlanner.Mode.SORTING; }
    private static Target target(Level level, BlockPos pos) { return new Target(level.getBlockEntity(pos) instanceof Container, level.getBlockState(pos).is(BlockTags.COPPER_CHESTS)); }
    private InteractionResult execute(Player player, CopperGolem golem, Level level, BlockPos pos, CopperWrenchInteractionPlanner.Intent intent) {
        CompoundTag tag = CopperGolemData.readEntityTag(golem); CopperGolemData.migrate(tag);
        CopperGolemBinding binding = new CopperGolemBinding(level.dimension(), pos.immutable()); boolean changed = switch (intent) {
            case REMOVE_SOURCE -> CopperWrenchStateMutator.removeSource(tag, binding);
            case REMOVE_BINDING -> CopperWrenchStateMutator.removeBinding(tag, binding);
            case TOGGLE_GATHERING_TARGET -> CopperWrenchStateMutator.toggleGatheringTarget(tag, BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).toString());
            case SET_SOURCE -> CopperWrenchStateMutator.setSource(tag, binding);
            case ADD_BINDING -> CopperWrenchStateMutator.addBinding(tag, binding);
            case SET_GATHERING_CORNER_A -> CopperWrenchStateMutator.setGatheringCorner(tag, level.dimension(), pos, false) == GatheringConfiguration.CornerUpdate.UPDATED;
            case SET_GATHERING_CORNER_B -> CopperWrenchStateMutator.setGatheringCorner(tag, level.dimension(), pos, true) == GatheringConfiguration.CornerUpdate.UPDATED;
            default -> false;
        };
        if (changed || intent != CopperWrenchInteractionPlanner.Intent.PASS) CopperGolemData.writeEntityTag(golem, tag);
        if (changed && level instanceof ServerLevel server && (intent == CopperWrenchInteractionPlanner.Intent.SET_SOURCE || intent == CopperWrenchInteractionPlanner.Intent.ADD_BINDING)) {
            CopperWrenchPathVisualization.show(server, golem, pos);
        }
        if (changed && player instanceof ServerPlayer server && (intent == CopperWrenchInteractionPlanner.Intent.SET_SOURCE || intent == CopperWrenchInteractionPlanner.Intent.ADD_BINDING)) bindingCriterion.trigger(server);
        CopperWrenchFeedback.send(player, intent, changed, BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).toString());
        return intent == CopperWrenchInteractionPlanner.Intent.PASS ? InteractionResult.PASS : InteractionResult.SUCCESS;
    }

    private record Target(boolean container, boolean copperSource) {
        CopperWrenchInteractionPlanner.Target plannerTarget() { return new CopperWrenchInteractionPlanner.Target(container, copperSource); }
    }
    @FunctionalInterface public interface MenuOpener { void open(ServerPlayer player, CopperGolem golem); }
    @FunctionalInterface public interface BindingCriterion { void trigger(ServerPlayer player); }
}
