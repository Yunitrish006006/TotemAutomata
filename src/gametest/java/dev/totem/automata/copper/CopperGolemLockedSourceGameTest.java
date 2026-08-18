package dev.totem.automata.copper;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.OptionalInt;

/** Proves denied Locksmith sources remain opaque to sorting and LLM inspection. */
public final class CopperGolemLockedSourceGameTest {
    private static final String ACCESS_BLOCKED = "totem_automata_sorting_access_blocked";
    private static final String SOURCE_HASH = "deadrecall_blocked_source_hash";
    private static final String BINDINGS_HASH = "deadrecall_blocked_bindings_hash";
    private static final String TARGETS_HASH = "deadrecall_blocked_targets_hash";

    @GameTest(maxTicks = 20)
    public void deniedSourceIsRejectedBeforeInventoryInspection(GameTestHelper helper) {
        CopperGolem golem = CopperGolemDirectInteractionGameTest.spawnCopperGolem(helper);
        if (golem == null) {
            helper.fail("Could not spawn a Copper Golem for locked-source policy test");
            return;
        }

        SimpleContainer source = new SimpleContainer(1);
        source.setItem(0, new ItemStack(Items.DIAMOND));
        ServerLevel level = helper.getLevel();
        BlockPos sourcePos = helper.absolutePos(new BlockPos(2, 2, 2));

        var tag = CopperGolemData.readEntityTag(golem);
        CopperGolemFuelService.writeFuelStack(tag, new ItemStack(Items.COAL), level);
        CopperGolemData.writeEntityTag(golem, tag);

        DenyingSortingOperations operations = new DenyingSortingOperations();
        ItemStack picked = SortingModeController.pickUp(golem, level, source, sourcePos, operations);

        if (!picked.isEmpty()) {
            helper.fail("Denied sorting source returned an item");
            return;
        }
        if (!source.getItem(0).is(Items.DIAMOND)) {
            helper.fail("Denied sorting source was modified");
            return;
        }
        if (operations.sourceInspected) {
            helper.fail("Denied sorting source reached sortableSourceSlot and exposed its contents");
            return;
        }
        if (!operations.accessBlocked) {
            helper.fail("Denied sorting source did not enter the access-blocked state");
            return;
        }

        var blockedTag = CopperGolemData.readEntityTag(golem);
        if (!blockedTag.getBooleanOr("deadrecall_sorting_blocked", false)
                || !blockedTag.getBooleanOr(ACCESS_BLOCKED, false)) {
            helper.fail("Denied sorting source did not persist its transient blocked markers");
            return;
        }
        if (blockedTag.contains(SOURCE_HASH)
                || blockedTag.contains(BINDINGS_HASH)
                || blockedTag.contains(TARGETS_HASH)) {
            helper.fail("Denied sorting source persisted content-derived hashes");
            return;
        }
        if (!operations.shouldClearBlocked(golem, level)) {
            helper.fail("Access-blocked sorting state did not schedule a permission-only retry");
            return;
        }
        helper.succeed();
    }

    private static final class DenyingSortingOperations extends PersistedSortingOperations {
        private boolean sourceInspected;
        private boolean accessBlocked;

        private DenyingSortingOperations() {
            super(new DefaultItemMetadata());
        }

        @Override
        public boolean mayExtract(CopperGolem golem, Container source) {
            return false;
        }

        @Override
        public OptionalInt sortableSourceSlot(
                CopperGolem golem,
                ServerLevel level,
                Container source,
                BlockPos sourcePos
        ) {
            sourceInspected = true;
            return OptionalInt.empty();
        }

        @Override
        public void markAccessBlocked(CopperGolem golem, ServerLevel level, BlockPos sourcePos) {
            accessBlocked = true;
            super.markAccessBlocked(golem, level, sourcePos);
        }
    }
}
