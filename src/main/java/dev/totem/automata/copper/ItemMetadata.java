package dev.totem.automata.copper;

import net.minecraft.world.item.ItemStack;
import java.util.List;

/** Server-side item identity/tags supplied by the live authority. */
public interface ItemMetadata {
    String itemId(ItemStack stack);
    List<String> itemTags(ItemStack stack);
    String itemName(ItemStack stack);
    String referenceTable();
}
