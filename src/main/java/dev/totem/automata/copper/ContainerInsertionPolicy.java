package dev.totem.automata.copper;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlastFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.block.entity.SmokerBlockEntity;

/** Shared slot validation for every Automata container-insertion path. */
final class ContainerInsertionPolicy {
    private static final int FURNACE_INPUT_SLOT = 0;
    private static final int FURNACE_FUEL_SLOT = 1;
    private static final int FURNACE_RESULT_SLOT = 2;

    private ContainerInsertionPolicy() { }

    static boolean canPlace(Container container, int slot, ItemStack stack) {
        if (stack.isEmpty() || slot < 0 || slot >= container.getContainerSize()
                || !container.canPlaceItem(slot, stack)) {
            return false;
        }
        if (!(container instanceof AbstractFurnaceBlockEntity furnace)) {
            return true;
        }
        if (slot == FURNACE_RESULT_SLOT) {
            return false;
        }
        if (slot == FURNACE_FUEL_SLOT) {
            return true;
        }
        if (slot != FURNACE_INPUT_SLOT) {
            return false;
        }

        Level level = furnace.getLevel();
        if (level == null || level.getServer() == null) {
            return false;
        }
        RecipeType<? extends AbstractCookingRecipe> recipeType = recipeType(furnace);
        return recipeType != null && hasNonEmptyResult(level, recipeType, stack);
    }

    static boolean isFurnaceLike(Container container) {
        return container instanceof AbstractFurnaceBlockEntity;
    }

    private static RecipeType<? extends AbstractCookingRecipe> recipeType(AbstractFurnaceBlockEntity furnace) {
        if (furnace instanceof BlastFurnaceBlockEntity) {
            return RecipeType.BLASTING;
        }
        if (furnace instanceof SmokerBlockEntity) {
            return RecipeType.SMOKING;
        }
        if (furnace instanceof FurnaceBlockEntity) {
            return RecipeType.SMELTING;
        }
        return null;
    }

    private static <T extends AbstractCookingRecipe> boolean hasNonEmptyResult(
            Level level,
            RecipeType<T> recipeType,
            ItemStack stack
    ) {
        SingleRecipeInput input = new SingleRecipeInput(stack.copyWithCount(1));
        return level.getServer().getRecipeManager().getRecipeFor(recipeType, input, level)
                .filter(holder -> !holder.value().assemble(input).isEmpty())
                .isPresent();
    }
}
