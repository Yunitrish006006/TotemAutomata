package dev.totem.automata.copper;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.animal.golem.CopperGolem;

/** Regression coverage for configured source vs. in-flight return-source state. */
public final class CopperGolemSourcePersistenceGameTest {
    @GameTest(maxTicks = 20)
    public void clearingTransportMemoryDoesNotUnbindConfiguredSource(GameTestHelper helper) {
        CopperGolem golem = CopperGolemDirectInteractionGameTest.spawnCopperGolem(helper);
        if (golem == null) {
            helper.fail("Could not spawn a Copper Golem for source persistence test");
            return;
        }

        BlockPos configuredPos = helper.absolutePos(new BlockPos(2, 2, 2));
        BlockPos rememberedPos = helper.absolutePos(new BlockPos(3, 2, 2));
        CopperGolemBinding configured = new CopperGolemBinding(helper.getLevel().dimension(), configuredPos);
        PersistedSortingOperations operations = new PersistedSortingOperations(new DefaultItemMetadata());

        var tag = CopperGolemData.readEntityTag(golem);
        SortingBindingService.writeSourceContainer(tag, configured);
        CopperGolemData.writeEntityTag(golem, tag);

        operations.rememberSource(golem, helper.getLevel(), rememberedPos, 4);
        SortingOperations.Source remembered = operations.rememberedSource(golem).orElse(null);
        if (remembered == null
                || !remembered.containerPos().equals(rememberedPos)
                || remembered.slot() != 4) {
            helper.fail("In-flight sorting source was not stored independently");
            return;
        }
        if (!SortingBindingService.getSourceContainer(CopperGolemData.readEntityTag(golem))
                .filter(configured::equals)
                .isPresent()) {
            helper.fail("Remembering an in-flight source overwrote the configured source");
            return;
        }

        operations.clearRememberedSource(golem);
        if (operations.rememberedSource(golem).isPresent()) {
            helper.fail("In-flight sorting source was not cleared after transport");
            return;
        }
        if (!SortingBindingService.getSourceContainer(CopperGolemData.readEntityTag(golem))
                .filter(configured::equals)
                .isPresent()) {
            helper.fail("Clearing in-flight transport memory unbound the configured source");
            return;
        }
        helper.succeed();
    }
}
