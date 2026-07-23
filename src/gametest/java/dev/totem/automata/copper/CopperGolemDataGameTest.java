package dev.totem.automata.copper;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;

import java.util.List;

/** Runs the persisted binding migration in the real Fabric GameTest runtime. */
public final class CopperGolemDataGameTest {
    @GameTest(maxTicks = 20)
    public void legacyBindingMigratesToList(GameTestHelper helper) {
        CompoundTag tag = new CompoundTag();
        tag.putString(CopperGolemData.TAG_BOUND_CONTAINER_DIM, Level.OVERWORLD.identifier().toString());
        tag.putInt(CopperGolemData.TAG_BOUND_CONTAINER_X, 12);
        tag.putInt(CopperGolemData.TAG_BOUND_CONTAINER_Y, 64);
        tag.putInt(CopperGolemData.TAG_BOUND_CONTAINER_Z, -8);
        if (!CopperGolemData.migrate(tag)) {
            helper.fail("Expected legacy Copper Golem binding migration to change data");
            return;
        }
        List<CopperGolemBinding> bindings = CopperGolemData.readBindings(tag);
        if (!bindings.equals(List.of(new CopperGolemBinding(Level.OVERWORLD, new BlockPos(12, 64, -8))))) {
            helper.fail("Legacy Copper Golem binding migration produced unexpected binding list");
            return;
        }
        helper.succeed();
    }
}
