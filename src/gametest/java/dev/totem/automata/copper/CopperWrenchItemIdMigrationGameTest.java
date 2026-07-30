package dev.totem.automata.copper;

import dev.totem.automata.registry.AutomataRegistries;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.List;
import java.util.UUID;

public final class CopperWrenchItemIdMigrationGameTest {
    @GameTest(maxTicks = 20)
    public void canonicalAndLegacyWrenchIdsRemainRegistered(GameTestHelper helper) {
        require(helper, "totem:automata/copper_wrench".equals(
                        BuiltInRegistries.ITEM.getKey(AutomataRegistries.COPPER_WRENCH).toString()),
                "Copper wrench canonical ID is incorrect");
        require(helper, "deadrecall:copper_wrench".equals(
                        BuiltInRegistries.ITEM.getKey(AutomataRegistries.LEGACY_COPPER_WRENCH).toString()),
                "Copper wrench legacy ID is no longer registered");
        helper.succeed();
    }

    @GameTest(maxTicks = 20)
    public void migrationPreservesSelectionAndCustomName(GameTestHelper helper) {
        UUID selectedGolem = UUID.randomUUID();
        Component customName = Component.literal("legacy wrench");
        ItemStack legacy = new ItemStack(AutomataRegistries.LEGACY_COPPER_WRENCH);
        legacy.set(DataComponents.CUSTOM_NAME, customName);
        require(helper, CopperWrenchSelection.select(legacy, selectedGolem),
                "Legacy wrench did not accept selection data");

        ItemStack migrated = CopperWrenchSelection.migrateLegacy(legacy);
        require(helper, migrated.is(AutomataRegistries.COPPER_WRENCH),
                "Legacy wrench did not migrate to canonical ID");
        require(helper, selectedGolem.equals(CopperWrenchSelection.selectedGolem(migrated)),
                "Wrench migration changed selected golem UUID");
        require(helper, customName.equals(migrated.get(DataComponents.CUSTOM_NAME)),
                "Wrench migration changed custom name");
        require(helper, legacy.is(AutomataRegistries.LEGACY_COPPER_WRENCH),
                "Wrench migration mutated the legacy source stack");
        helper.succeed();
    }

    @SuppressWarnings("removal")
    @GameTest(maxTicks = 20)
    public void legacyWrenchMigratesBeforeGolemInteraction(GameTestHelper helper) {
        var golem = CopperGolemDirectInteractionGameTest.spawnCopperGolem(helper);
        require(helper, golem != null, "Could not spawn copper golem");
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        ItemStack legacy = new ItemStack(AutomataRegistries.LEGACY_COPPER_WRENCH);
        player.setItemInHand(InteractionHand.MAIN_HAND, legacy);
        try {
            PersistedCopperWrenchInteractionAuthority authority =
                    new PersistedCopperWrenchInteractionAuthority((viewer, target) -> { });
            authority.useEntity(player, helper.getLevel(), InteractionHand.MAIN_HAND, golem);

            ItemStack migrated = player.getMainHandItem();
            require(helper, migrated.is(AutomataRegistries.COPPER_WRENCH),
                    "Golem interaction did not migrate held wrench");
            require(helper, golem.getUUID().equals(CopperWrenchSelection.selectedGolem(migrated)),
                    "Golem interaction did not retain selection on canonical wrench");
            helper.succeed();
        } finally {
            player.discard();
            golem.discard();
        }
    }

    @GameTest(maxTicks = 20)
    public void craftingRecipeProducesCanonicalWrench(GameTestHelper helper) {
        CraftingInput input = CraftingInput.of(3, 3, List.of(
                ItemStack.EMPTY, new ItemStack(Items.COPPER_INGOT), ItemStack.EMPTY,
                ItemStack.EMPTY, new ItemStack(Items.COPPER_INGOT), new ItemStack(Items.COPPER_INGOT),
                new ItemStack(Items.STICK), ItemStack.EMPTY, ItemStack.EMPTY
        ));
        RecipeHolder<CraftingRecipe> recipe = helper.getLevel().recipeAccess()
                .getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel())
                .orElseThrow(() -> helper.assertionException("Copper wrench crafting recipe is missing"));
        require(helper, recipe.value().assemble(input).is(AutomataRegistries.COPPER_WRENCH),
                "Copper wrench recipe did not produce canonical ID");
        helper.succeed();
    }

    private static void require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) throw helper.assertionException(message);
    }
}
