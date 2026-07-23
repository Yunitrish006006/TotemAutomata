package dev.totem.automata.copper;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Fuel accounting for one Copper Golem transport operation. */
public final class CopperGolemFuelService {
    public static final int FUEL_TICKS_PER_TRANSPORT = 200;

    private CopperGolemFuelService() {
    }

    public static ItemStack readFuelStack(CompoundTag tag) {
        return CopperGolemData.readItemStack(tag, CopperGolemData.TAG_FUEL_STACK);
    }

    public static void writeFuelStack(CompoundTag tag, ItemStack fuelStack) {
        CopperGolemData.writeItemStack(tag, CopperGolemData.TAG_FUEL_STACK, fuelStack);
    }

    public static boolean hasFuelAvailable(CompoundTag tag, ServerLevel level) {
        return tag.getIntOr(CopperGolemData.TAG_FUEL_TICKS, 0) > 0 || isFuel(level, readFuelStack(tag));
    }

    public static boolean consumeForTransport(CompoundTag tag, ServerLevel level) {
        int fuelTicks = tag.getIntOr(CopperGolemData.TAG_FUEL_TICKS, 0);
        if (fuelTicks <= 0) {
            ItemStack fuelStack = readFuelStack(tag);
            if (!isFuel(level, fuelStack)) return false;
            fuelTicks = Math.max(1, level.fuelValues().burnDuration(fuelStack));
            writeFuelStack(tag, consumeOneFuelItem(fuelStack));
        }
        fuelTicks = Math.max(0, fuelTicks - FUEL_TICKS_PER_TRANSPORT);
        if (fuelTicks > 0) tag.putInt(CopperGolemData.TAG_FUEL_TICKS, fuelTicks);
        else tag.remove(CopperGolemData.TAG_FUEL_TICKS);
        return true;
    }

    public static boolean isFuel(ServerLevel level, ItemStack stack) {
        return !stack.isEmpty() && level.fuelValues().isFuel(stack);
    }

    private static ItemStack consumeOneFuelItem(ItemStack fuelStack) {
        Item item = fuelStack.getItem();
        fuelStack.shrink(1);
        if (!fuelStack.isEmpty()) return fuelStack;
        var craftingRemainder = item.getCraftingRemainder();
        return craftingRemainder == null ? ItemStack.EMPTY : craftingRemainder.create();
    }
}
