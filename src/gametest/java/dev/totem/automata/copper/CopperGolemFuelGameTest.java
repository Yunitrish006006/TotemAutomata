package dev.totem.automata.copper;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Exercises Copper Golem fuel persistence in the real Fabric GameTest runtime. */
public final class CopperGolemFuelGameTest {
    @GameTest(maxTicks = 20)
    public void fuelConsumptionPersistsRemainingBurnTicks(GameTestHelper helper) {
        CompoundTag tag = new CompoundTag();
        CopperGolemFuelService.writeFuelStack(tag, new ItemStack(Items.COAL), helper.getLevel());
        if (!CopperGolemFuelService.consumeForTransport(tag, helper.getLevel())) {
            helper.fail("Copper Golem rejected a valid coal fuel stack");
            return;
        }
        int remainingTicks = tag.getIntOr(CopperGolemData.TAG_FUEL_TICKS, 0);
        if (remainingTicks <= 0 || !CopperGolemFuelService.readFuelStack(tag, helper.getLevel()).isEmpty()) {
            helper.fail("Copper Golem fuel consumption did not preserve burn ticks and consume coal exactly once");
            return;
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 20)
    public void netherStarProvidesInfiniteFuelWithoutMutatingFiniteState(GameTestHelper helper) {
        CompoundTag tag = new CompoundTag();
        tag.putInt(CopperGolemData.TAG_FUEL_TICKS, 731);
        CopperGolemFuelService.writeFuelStack(
                tag, new ItemStack(Items.NETHER_STAR, 2), helper.getLevel());

        for (int cycle = 0; cycle < 8; cycle++) {
            if (!CopperGolemFuelService.hasFuelAvailable(tag, helper.getLevel())
                    || !CopperGolemFuelService.consumeForTransport(tag, helper.getLevel())) {
                helper.fail("Nether Star stopped powering repeated Copper Golem work");
                return;
            }
        }

        ItemStack remaining = CopperGolemFuelService.readFuelStack(tag, helper.getLevel());
        if (!remaining.is(Items.NETHER_STAR) || remaining.getCount() != 2
                || tag.getIntOr(CopperGolemData.TAG_FUEL_TICKS, 0) != 731) {
            helper.fail("Infinite fuel consumed a Nether Star or changed paused finite burn state");
            return;
        }
        CopperGolemFuelService.writeFuelStack(tag, ItemStack.EMPTY, helper.getLevel());
        if (!CopperGolemFuelService.consumeForTransport(tag, helper.getLevel())
                || tag.getIntOr(CopperGolemData.TAG_FUEL_TICKS, 0) != 531) {
            helper.fail("Removing infinite fuel did not resume the preserved finite counter");
            return;
        }
        helper.succeed();
    }
}
