package dev.totem.automata.copper;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/** Exercises the additive extraction's sorting, persistence and request-pressure primitives in Fabric. */
public final class CopperGolemMigrationGameTest {
    @GameTest(maxTicks = 20)
    public void sortingMergesCompatibleStacksBeforeUsingAnEmptySlot(GameTestHelper helper) {
        NonNullList<ItemStack> inventory = NonNullList.withSize(3, ItemStack.EMPTY);
        inventory.set(0, new ItemStack(Items.COBBLESTONE, 60));
        ItemStack remainder = BackpackSortingHelper.insertInto(inventory, new ItemStack(Items.COBBLESTONE, 8));
        if (!remainder.isEmpty() || inventory.get(0).getCount() != 64
                || inventory.get(1).getItem() != Items.COBBLESTONE || inventory.get(1).getCount() != 4) {
            helper.fail("Sorting did not merge first and preserve the expected remainder slot");
            return;
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 20)
    public void bindingConfigurationSurvivesACodecFreeVirtualRestart(GameTestHelper helper) {
        CompoundTag persisted = new CompoundTag();
        List<CopperGolemBinding> expected = List.of(
                new CopperGolemBinding(Level.OVERWORLD, new BlockPos(3, 70, -5)),
                new CopperGolemBinding(Level.NETHER, new BlockPos(-2, 64, 9)));
        CopperGolemData.writeBindings(persisted, expected);
        CompoundTag restarted = persisted.copy();
        if (!CopperGolemData.readBindings(restarted).equals(expected)) {
            helper.fail("Copper Golem bindings did not survive a virtual restart round trip");
            return;
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 20)
    public void requestGateRejectsDuplicateWorkUntilTheBackoffExpires(GameTestHelper helper) {
        AtomicLong clock = new AtomicLong(100L);
        LlmRequestGate gate = new LlmRequestGate(25L, clock::get);
        String key = "gather:pressure";
        if (!gate.tryStart(key) || gate.tryStart(key)) {
            helper.fail("Request gate allowed concurrent duplicate gathering work");
            return;
        }
        gate.completeFailure(key);
        if (gate.tryStart(key)) {
            helper.fail("Request gate ignored its retry backoff under pressure");
            return;
        }
        clock.set(125L);
        if (!gate.tryStart(key)) {
            helper.fail("Request gate did not reopen after its retry backoff");
            return;
        }
        helper.succeed();
    }
}
