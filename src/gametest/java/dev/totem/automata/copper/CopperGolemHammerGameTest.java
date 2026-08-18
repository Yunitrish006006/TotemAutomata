package dev.totem.automata.copper;

import dev.totem.automata.excavation.TotemExcavationHammerAdapter;
import dev.totem.automata.menu.PersistedCopperGolemMenuAuthority;
import dev.totem.excavation.HammerTier;
import dev.totem.excavation.component.AreaSelection;
import dev.totem.excavation.component.ExcavationDataComponents;
import dev.totem.excavation.registry.ExcavationItems;
import dev.totem.excavation.session.ExcavationSessions;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

/** Runtime contract tests for the optional Totem Excavation gathering adapter. */
public final class CopperGolemHammerGameTest {
    private static final String TOOL = "deadrecall_gathering_tool_stack";
    private static final String STORAGE = "deadrecall_gathering_storage_stack";

    @GameTest(maxTicks = 40)
    public void canonicalHammersAreAcceptedWithoutLosingComponents(GameTestHelper helper) {
        require(helper, TotemExcavationHammerAdapter.isAvailable(),
                "Totem Excavation was not present in the integration GameTest runtime");
        PersistedCopperGolemMenuAuthority authority = new PersistedCopperGolemMenuAuthority((viewer, golem) -> { });
        for (HammerTier tier : HammerTier.values()) {
            ItemStack canonical = new ItemStack(item("totem:excavation/" + tier.path() + "_hammer"));
            require(helper, TotemExcavationHammerAdapter.isSupported(canonical),
                    "Canonical " + tier.path() + " hammer was not recognised");
            require(helper, authority.isGatheringTool(canonical),
                    "Menu rejected canonical " + tier.path() + " hammer");
        }
        require(helper, !authority.isGatheringTool(new ItemStack(Items.STICK)), "Menu accepted a stick as a gathering tool");
        require(helper, !authority.isGatheringTool(new ItemStack(Items.COPPER_INGOT)), "Menu accepted an ingot as a gathering tool");

        CopperGolem golem = CopperGolemDirectInteractionGameTest.spawnCopperGolem(helper);
        require(helper, golem != null, "Could not spawn a Copper Golem for hammer storage");
        ItemStack configured = new ItemStack(ExcavationItems.DIAMOND_HAMMER);
        configured.setDamageValue(7);
        configured.set(DataComponents.CUSTOM_NAME, Component.literal("Configured hammer"));
        AreaSelection selection = AreaSelection.firstCorner(Level.OVERWORLD, new BlockPos(2, 64, 2));
        configured.set(ExcavationDataComponents.AREA_SELECTION, selection);
        authority.setGatheringTool(golem, configured);
        ItemStack stored = authority.gatheringTool(golem);
        require(helper, stored.getCount() == 1 && ItemStack.isSameItemSameComponents(stored, configured),
                "Copper Golem did not preserve the configured hammer stack");
        require(helper, selection.equals(stored.get(ExcavationDataComponents.AREA_SELECTION)),
                "Copper Golem lost the hammer selection Component");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void hammerHarvestUsesOneTargetTransactionAndHonoursHammerTags(GameTestHelper helper) {
        CopperGolem golem = CopperGolemDirectInteractionGameTest.spawnCopperGolem(helper);
        require(helper, golem != null, "Could not spawn a Copper Golem for hammer harvesting");
        ServerLevel level = helper.getLevel();
        ServerPlayer operator = helper.makeMockServerPlayerInLevel();
        BlockPos targetRelative = new BlockPos(3, 2, 3);
        BlockPos neighbourRelative = targetRelative.offset(1, 0, 0);
        BlockPos target = helper.absolutePos(targetRelative);
        helper.setBlock(targetRelative, Blocks.OAK_PLANKS);
        helper.setBlock(neighbourRelative, Blocks.OAK_PLANKS);

        ItemStack hammer = new ItemStack(ExcavationItems.NETHERITE_HAMMER);
        AreaSelection selection = AreaSelection.firstCorner(Level.OVERWORLD, new BlockPos(8, 65, 8));
        hammer.set(ExcavationDataComponents.AREA_SELECTION, selection);
        var efficiency = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.EFFICIENCY);
        EnchantmentHelper.updateEnchantments(hammer, enchantments -> enchantments.set(efficiency, 3));
        require(helper, GatheringDrops.resolve(golem, level, target, Blocks.OAK_PLANKS.defaultBlockState(), hammer).isPresent(),
                "A hammer rejected an eligible planks target");
        require(helper, GatheringDrops.resolve(golem, level, target, Blocks.GLASS.defaultBlockState(), hammer).isEmpty(),
                "A hammer accepted a block outside Totem Excavation's mining tag");

        var tag = CopperGolemData.readEntityTag(golem);
        CopperGolemData.writeItemStack(tag, TOOL, hammer, level.registryAccess());
        CopperGolemData.writeItemStack(tag, STORAGE, ItemStack.EMPTY, level.registryAccess());
        CopperGolemFuelService.writeFuelStack(tag, new ItemStack(Items.COAL), level);
        CopperGolemData.writeEntityTag(golem, tag);

        try {
            require(helper, !ExcavationSessions.isHarvesting(operator),
                    "Copper Golem started a player hammer session before its own transaction");
            GatheringBlockBreaker.Result result = GatheringBlockBreaker.breakTarget(golem, level, operator, target);
            require(helper, result == GatheringBlockBreaker.Result.BROKEN,
                    "Authorised Copper Golem hammer harvest was rejected: " + result);
            require(helper, level.getBlockState(target).isAir(), "Hammer harvest did not remove exactly its target block");
            require(helper, level.getBlockState(helper.absolutePos(neighbourRelative)).is(Blocks.OAK_PLANKS),
                    "Hammer harvest escaped the Copper Golem's one-target transaction");
            var after = CopperGolemData.readEntityTag(golem);
            ItemStack storedHammer = CopperGolemData.readItemStack(after, TOOL, level.registryAccess());
            ItemStack storage = CopperGolemData.readItemStack(after, STORAGE, level.registryAccess());
            require(helper, storedHammer.getDamageValue() > hammer.getDamageValue(),
                    "Successful hammer harvest did not apply durability");
            require(helper, selection.equals(storedHammer.get(ExcavationDataComponents.AREA_SELECTION)),
                    "Hammer harvest changed the stored player-owned selection Component");
            require(helper, EnchantmentHelper.getItemEnchantmentLevel(efficiency, storedHammer) == 3,
                    "Hammer harvest lost the stored efficiency enchantment");
            require(helper, !ExcavationSessions.isHarvesting(operator),
                    "Copper Golem hammer harvesting entered a player excavation session");
            require(helper, storage.is(Blocks.OAK_PLANKS.asItem()) && storage.getCount() == 1,
                    "Hammer drops were not committed to Copper Golem storage exactly once");
            require(helper, CopperGolemFuelService.readFuelStack(after, level).isEmpty()
                            || after.getIntOr(CopperGolemData.TAG_FUEL_TICKS, 0) > 0,
                    "Successful hammer harvest did not consume or convert its fuel");
            helper.succeed();
        } finally {
            operator.discard();
        }
    }

    private static Item item(String id) {
        Item value = BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
        if (value == null) {
            throw new IllegalStateException("Missing registered item " + id);
        }
        return value;
    }

    private static void require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            throw helper.assertionException(message);
        }
    }
}
