package dev.totem.automata.copper;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CookingFuel;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Fuel accounting for one Copper Golem transport operation. */
public final class CopperGolemFuelService {
    public static final int FUEL_TICKS_PER_TRANSPORT = 200;

    private CopperGolemFuelService() {
    }

    public static ItemStack readFuelStack(CompoundTag tag, ServerLevel level) {
        return CopperGolemData.readItemStack(tag, CopperGolemData.TAG_FUEL_STACK, level.registryAccess());
    }

    public static void writeFuelStack(CompoundTag tag, ItemStack fuelStack, ServerLevel level) {
        CopperGolemData.writeItemStack(tag, CopperGolemData.TAG_FUEL_STACK, fuelStack, level.registryAccess());
    }

    public static boolean hasFuelAvailable(CompoundTag tag, ServerLevel level) {
        return tag.getIntOr(CopperGolemData.TAG_FUEL_TICKS, 0) > 0
                || isFuel(level, readFuelStack(tag, level));
    }

    public static boolean consumeForTransport(CompoundTag tag, ServerLevel level) {
        if (isInfiniteFuel(readFuelStack(tag, level))) {
            return true;
        }
        int fuelTicks = tag.getIntOr(CopperGolemData.TAG_FUEL_TICKS, 0);
        if (fuelTicks <= 0) {
            ItemStack fuelStack = readFuelStack(tag, level);
            if (!isFuel(level, fuelStack)) return false;
            LootContext context = new LootContext.Builder(
                    new LootParams.Builder(level).create(LootContextParamSets.EMPTY)).create(Optional.empty());
            fuelTicks = ResolvableInt.getFromItem(fuelStack, DataComponents.COOKING_FUEL,
                    CookingFuel::burnTime, context, 0);
            if (fuelTicks <= 0) return false;
            writeFuelStack(tag, consumeOneFuelItem(fuelStack), level);
        }
        fuelTicks = Math.max(0, fuelTicks - FUEL_TICKS_PER_TRANSPORT);
        if (fuelTicks > 0) tag.putInt(CopperGolemData.TAG_FUEL_TICKS, fuelTicks);
        else tag.remove(CopperGolemData.TAG_FUEL_TICKS);
        return true;
    }

    public static boolean isFuel(ServerLevel level, ItemStack stack) {
        return isInfiniteFuel(stack)
                || !stack.isEmpty() && stack.has(DataComponents.COOKING_FUEL);
    }

    public static boolean isInfiniteFuel(ItemStack stack) {
        return stack.is(Items.NETHER_STAR);
    }

    private static ItemStack consumeOneFuelItem(ItemStack fuelStack) {
        Item item = fuelStack.getItem();
        fuelStack.shrink(1);
        if (!fuelStack.isEmpty()) return fuelStack;
        var craftingRemainder = item.getCraftingRemainder();
        return craftingRemainder == null ? ItemStack.EMPTY : craftingRemainder.create();
    }
}
