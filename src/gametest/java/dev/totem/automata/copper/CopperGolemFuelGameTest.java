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
        CopperGolemFuelService.writeFuelStack(tag, new ItemStack(Items.COAL));
        if (!CopperGolemFuelService.consumeForTransport(tag, helper.getLevel())) {
            helper.fail("Copper Golem rejected a valid coal fuel stack");
            return;
        }
        int remainingTicks = tag.getIntOr(CopperGolemData.TAG_FUEL_TICKS, 0);
        if (remainingTicks <= 0 || !CopperGolemFuelService.readFuelStack(tag).isEmpty()) {
            helper.fail("Copper Golem fuel consumption did not preserve burn ticks and consume coal exactly once");
            return;
        }
        helper.succeed();
    }
}
