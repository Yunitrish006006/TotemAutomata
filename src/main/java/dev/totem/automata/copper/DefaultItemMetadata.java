package dev.totem.automata.copper;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Vanilla registry-backed metadata used by the sorting classifier. */
public final class DefaultItemMetadata implements ItemMetadata {
    private static final String REFERENCES = """
            Keyword reference for container prompts:
            - 礦物 / 礦石 / 金屬: ores, raw ores, ingots, nuggets, gems, coal, charcoal, redstone, lapis, quartz, amethyst, copper, iron, gold, diamond, emerald, netherite materials.
            - 食物 / 料理: edible items, bread, cooked or raw meat, fish, fruit, vegetables, soup, stew, cake, pie, cookies.
            - 工具: tools and usable work items such as pickaxes, axes, shovels, hoes, shears, fishing rods, brushes, flint and steel, buckets, compasses, clocks. Do not include raw crafting materials unless the prompt also asks for materials.
            - 作物 / 農作物: crops, seeds, wheat, carrots, potatoes, beetroot, melon, pumpkin, sugar cane, bamboo, cactus, cocoa beans, nether wart, farming produce.
            - 動物 / 動物掉落: animal-related drops and products such as meat, leather, wool, feathers, eggs, rabbit hide, scutes, milk buckets, ink sacs.
            - 材料 / 合成材料: general crafting ingredients and intermediate materials such as sticks, string, paper, leather, dyes, bone meal, slime balls, honeycomb, blaze powder, gunpowder, clay balls.
            - 建材 / 方塊 / 裝飾: building and decoration blocks such as stone, cobblestone, deepslate, dirt, sand, gravel, wood blocks, planks, bricks, concrete, glass, stairs, slabs, walls, doors, fences, lanterns.
            - 畜牧 / 牧場: animal husbandry and farm-animal management items such as animal feed, wheat, seeds, carrots, potatoes, beetroot, hay bales, leads, name tags, saddles, shears, buckets, eggs.
            Use these references only as interpretation help. The player's container prompt has priority.
            """;
    @Override public String itemId(ItemStack stack) { return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(); }
    @Override public List<String> itemTags(ItemStack stack) { return stack.typeHolder().tags().map(tag -> tag.location().toString()).sorted().toList(); }
    @Override public String itemName(ItemStack stack) { return stack.getHoverName().getString(); }
    @Override public String referenceTable() { return REFERENCES; }
}
