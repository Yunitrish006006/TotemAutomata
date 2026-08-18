package dev.totem.automata.client;

import dev.totem.excavation.registry.ExcavationItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/**
 * Screenshot-only study that composes the actual Java furnace background and
 * progress sprites with Copper Golem state. It has no server behaviour.
 */
final class VanillaCopperGolemBackpackPrototypeScreen extends Screen {
    private static final int PANEL_WIDTH = 176;
    private static final int PANEL_HEIGHT = 222;
    private static final Identifier FURNACE_TEXTURE = Identifier.withDefaultNamespace("textures/gui/container/furnace.png");
    private static final Identifier LIT_PROGRESS = Identifier.withDefaultNamespace("container/furnace/lit_progress");
    private static final Identifier SLOT = Identifier.withDefaultNamespace("container/slot");
    private static final Identifier TOOL_SLOT = Identifier.withDefaultNamespace("container/slot/pickaxe");
    private static final Identifier COPPER_CHEST_ID = Identifier.fromNamespaceAndPath("minecraft", "copper_chest");
    private static final int SORTING_TARGET_COUNT = 5;
    private static final int SORTING_TARGET_SPACING = 18;
    private CopperGolem previewGolem;
    private boolean targetBlocksVisible;
    private boolean llmPanelVisible;
    private boolean llmEnabled = true;
    private boolean stopped;
    private boolean sortingMode;
    private boolean sortingCachePanelVisible;
    private boolean cacheValueIsTag;
    private boolean cacheValueAllowed = true;
    private int sortingTargetCount = SORTING_TARGET_COUNT;
    private int selectedSortingTarget;
    private final int[] allowedCacheEntries = {1, 2, 0, 4, 1};
    private final int[] deniedCacheEntries = {1, 0, 3, 0, 2};

    VanillaCopperGolemBackpackPrototypeScreen() {
        super(Component.literal("銅魁儡"));
    }

    void setSortingTargetCountForVisualTest(int targetCount) {
        sortingMode = true;
        sortingCachePanelVisible = false;
        sortingTargetCount = Math.max(1, Math.min(targetCount, SORTING_TARGET_COUNT));
        selectedSortingTarget = 0;
    }

    @Override
    protected void init() {
        int x = (width - PANEL_WIDTH) / 2;
        int y = (height - PANEL_HEIGHT) / 2;
        if (Minecraft.getInstance().level != null) {
            var type = BuiltInRegistries.ENTITY_TYPE.getValue(
                    Identifier.fromNamespaceAndPath("minecraft", "copper_golem"));
            if (type != null) {
                Entity entity = type.create(Minecraft.getInstance().level, EntitySpawnReason.COMMAND);
                if (entity instanceof CopperGolem golem) {
                    golem.setId(1_000_000);
                    previewGolem = golem;
                }
            }
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tick) {
        graphics.fill(0, 0, width, height, 0xC0101010);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int x = (width - PANEL_WIDTH) / 2;
        int y = (height - PANEL_HEIGHT) / 2;
        if (event.button() == 0 && inBounds(event.x(), event.y(), x + 104, y + 3, 18, 18)) {
            stopped = !stopped;
            return true;
        }
        if (event.button() == 0 && inBounds(event.x(), event.y(), x + 128, y + 3, 18, 18)) {
            sortingMode = !sortingMode;
            if (!sortingMode) sortingCachePanelVisible = false;
            return true;
        }
        if (event.button() == 0 && inBounds(event.x(), event.y(), x + 152, y + 3, 18, 18)) {
            llmPanelVisible = !llmPanelVisible;
            return true;
        }
        if (llmPanelVisible && event.button() == 0 && inBounds(event.x(), event.y(), x + 143, y + 31, 18, 18)) {
            llmEnabled = !llmEnabled;
            return true;
        }
        if (sortingMode && sortingCachePanelVisible && event.button() == 0) {
            if (inBounds(event.x(), event.y(), x + 82, y + 52, 18, 18)) {
                cacheValueIsTag = !cacheValueIsTag;
                return true;
            }
            if (inBounds(event.x(), event.y(), x + 82, y + 72, 84, 16)) {
                cacheValueAllowed = true;
                allowedCacheEntries[selectedSortingTarget]++;
                return true;
            }
            if (inBounds(event.x(), event.y(), x + 82, y + 90, 84, 16)) {
                cacheValueAllowed = false;
                deniedCacheEntries[selectedSortingTarget]++;
                return true;
            }
        }
        int targetIndex = sortingTargetAt(event.x(), event.y(), x, y);
        if (sortingMode && !llmPanelVisible && !sortingCachePanelVisible && event.button() == 0 && targetIndex >= 0) {
            selectedSortingTarget = targetIndex;
            sortingCachePanelVisible = true;
            return true;
        }
        if (event.button() == 0 && inTargetBubble(event.x(), event.y(), x, y)) {
            targetBlocksVisible = !targetBlocksVisible;
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private static boolean inBounds(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private boolean inTargetBubble(double mouseX, double mouseY, int x, int y) {
        return targetBlocksVisible
                ? inBounds(mouseX, mouseY, x + 33, y + 24, 54, 28)
                : inBounds(mouseX, mouseY, x + 48, y + 34, 28, 18);
    }

    private int sortingTargetAt(double mouseX, double mouseY, int x, int y) {
        if (mouseX < x + 146 || mouseX >= x + 164) return -1;
        for (int index = 0; index < sortingTargetCount; index++) {
            int targetY = sortingTargetY(y, index);
            if (mouseY >= targetY && mouseY < targetY + 18) return index;
        }
        return -1;
    }

    private int sortingTargetY(int y, int index) {
        return y + 70 - (sortingTargetCount - 1) * (SORTING_TARGET_SPACING / 2)
                + index * SORTING_TARGET_SPACING;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tick) {
        int x = (width - PANEL_WIDTH) / 2;
        int y = (height - PANEL_HEIGHT) / 2;
        drawCustomTop(graphics, x, y);
        graphics.blit(RenderPipelines.GUI_TEXTURED, FURNACE_TEXTURE, x, y + 126, 0.0F, 70.0F,
                PANEL_WIDTH, PANEL_HEIGHT - 126, 256, 256);
        graphics.text(font, title, x + 8, y + 9, 0xFF404040, false);

        if (llmPanelVisible) {
            renderLlmPanel(graphics, x, y);
        } else {
            if (!sortingCachePanelVisible) {
                graphics.text(font, Component.literal(stopped ? "● 已停止" : sortingMode ? "● 整理中" : "● 採集中"),
                        x + 92, y + 29, stopped ? 0xFF9B3030 : 0xFF287C35, false);
            }
            if (sortingMode) {
                if (sortingCachePanelVisible) renderSortingCachePanel(graphics, x, y);
                else renderSortingPanel(graphics, x, y);
            } else {
                renderSlot(graphics, SLOT, x + 96, y + 42);
                renderSlot(graphics, TOOL_SLOT, x + 96, y + 42);
                renderSlot(graphics, SLOT, x + 96, y + 71);
                renderItem(graphics, new ItemStack(ExcavationItems.DIAMOND_HAMMER), x + 96, y + 42);
                renderItem(graphics, new ItemStack(Items.COAL, 4), x + 96, y + 71);
                renderFuelRemaining(graphics, x, y);
            }
        }
        if (previewGolem != null) {
            int previewLeft = sortingMode && !llmPanelVisible && !sortingCachePanelVisible ? x + 50 : x + 8;
            int previewRight = sortingMode && !llmPanelVisible && !sortingCachePanelVisible ? x + 124 : x + 78;
            InventoryScreen.extractEntityInInventoryFollowsMouse(graphics,
                    previewLeft, y + 24, previewRight, y + 118, 30, 0.0625F, mouseX, mouseY, previewGolem);
        }
        if (!llmPanelVisible && !sortingMode) {
            renderThoughtBubble(graphics, x, y);
            renderHomeChest(graphics, x, y);
        }
        renderIconAction(graphics, new ItemStack(Items.BARRIER), x + 104, y + 3);
        renderIconAction(graphics, new ItemStack(Items.COMPASS), x + 128, y + 3);
        renderIconAction(graphics, new ItemStack(Items.WRITABLE_BOOK), x + 152, y + 3);
        renderIconTooltip(graphics, x, y, mouseX, mouseY);
        if (llmPanelVisible) renderLlmToggleTooltip(graphics, x, y, mouseX, mouseY);
        if (sortingMode) {
            renderSortingTargetTooltip(graphics, x, y, mouseX, mouseY);
            renderSortingCacheTooltip(graphics, x, y, mouseX, mouseY);
        }
        super.extractRenderState(graphics, mouseX, mouseY, tick);
    }

    private void drawCustomTop(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.fill(x, y, x + PANEL_WIDTH, y + 126, 0xFFC6C6C6);
        graphics.fill(x + 2, y + 2, x + PANEL_WIDTH - 2, y + 3, 0xFFFFFFFF);
        graphics.fill(x + 2, y + 124, x + PANEL_WIDTH - 2, y + 126, 0xFF555555);
        graphics.verticalLine(x, y, y + 125, 0xFF000000);
        graphics.verticalLine(x + PANEL_WIDTH - 1, y, y + 125, 0xFF000000);
        graphics.horizontalLine(x, x + PANEL_WIDTH - 1, y, 0xFF000000);
    }

    private void renderSlot(GuiGraphicsExtractor graphics, Identifier sprite, int x, int y) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, 18, 18);
    }

    private void renderItem(GuiGraphicsExtractor graphics, ItemStack stack, int x, int y) {
        graphics.item(stack, x, y);
        graphics.itemDecorations(font, stack, x, y);
    }

    private void renderFuelRemaining(GuiGraphicsExtractor graphics, int x, int y) {
        int flameHeight = 10;
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, LIT_PROGRESS,
                14, 14, 0, 14 - flameHeight,
                x + 119, y + 72 + 14 - flameHeight, 14, flameHeight);
    }

    private void renderThoughtBubble(GuiGraphicsExtractor graphics, int x, int y) {
        if (targetBlocksVisible) {
            graphics.fill(x + 33, y + 24, x + 87, y + 52, 0xFFFFFFFF);
            graphics.outline(x + 33, y + 24, 54, 28, 0xFF555555);
            renderItem(graphics, new ItemStack(Blocks.COPPER_ORE), x + 36, y + 30);
            renderItem(graphics, new ItemStack(Blocks.DEEPSLATE_COPPER_ORE), x + 53, y + 30);
            renderItem(graphics, new ItemStack(Blocks.RAW_COPPER_BLOCK), x + 70, y + 30);
        } else {
            graphics.fill(x + 48, y + 34, x + 76, y + 52, 0xFFFFFFFF);
            graphics.outline(x + 48, y + 34, 28, 18, 0xFF555555);
            graphics.text(font, Component.literal("…"), x + 58, y + 38, 0xFF555555, false);
        }
        // Pixel tail connecting the thought bubble to the Copper Golem preview.
        graphics.fill(x + 45, y + 51, x + 50, y + 56, 0xFFFFFFFF);
        graphics.fill(x + 41, y + 55, x + 44, y + 58, 0xFFFFFFFF);
    }

    private void renderHomeChest(GuiGraphicsExtractor graphics, int x, int y) {
        int chestX = x + 61;
        int chestY = y + 99;
        graphics.text(font, Component.literal("歸位"), x + 57, y + 88, 0xFF555555, false);
        graphics.verticalLine(x + 40, y + 85, y + 108, 0xFF555555);
        graphics.horizontalLine(x + 40, x + 55, y + 108, 0xFF555555);
        renderArrowHead(graphics, x + 55, y + 108);
        renderSlot(graphics, SLOT, chestX, chestY);
        var chestItem = BuiltInRegistries.ITEM.getValue(COPPER_CHEST_ID);
        renderItem(graphics, new ItemStack(chestItem != null ? chestItem : Items.CHEST), chestX, chestY);
    }

    private void renderSortingPanel(GuiGraphicsExtractor graphics, int x, int y) {
        int sourceX = x + 12;
        int targetX = x + 146;
        int sourceY = y + 70;
        int trunkX = x + 133;
        int flowY = sourceY + 9;
        graphics.text(font, Component.literal("來源"), sourceX, y + 58, 0xFF555555, false);
        graphics.text(font, Component.literal("目標"), targetX, y + 24, 0xFF555555, false);
        renderSlot(graphics, SLOT, sourceX, sourceY);
        var copperChest = BuiltInRegistries.ITEM.getValue(COPPER_CHEST_ID);
        renderItem(graphics, new ItemStack(copperChest != null ? copperChest : Items.CHEST), sourceX, sourceY);
        graphics.horizontalLine(sourceX + 20, x + 49, flowY, 0xFF555555);
        renderArrowHead(graphics, x + 45, flowY);
        graphics.horizontalLine(x + 116, trunkX, flowY, 0xFF555555);
        graphics.verticalLine(trunkX, sortingTargetY(y, 0) + 9,
                sortingTargetY(y, sortingTargetCount - 1) + 9, 0xFF555555);
        for (int index = 0; index < sortingTargetCount; index++) {
            int targetY = sortingTargetY(y, index);
            int targetFlowY = targetY + 9;
            renderSlot(graphics, SLOT, targetX, targetY);
            renderItem(graphics, sortingTargetStack(index), targetX, targetY);
            if (index == selectedSortingTarget) {
                graphics.outline(targetX - 1, targetY - 1, 20, 20, 0xFFE0C24D);
            }
            graphics.horizontalLine(trunkX, targetX - 3, targetFlowY, 0xFF555555);
            renderArrowHead(graphics, targetX - 7, targetFlowY);
        }
        graphics.text(font, Component.literal(sortingTargetCount + " 個綁定目標"),
                x + 59, y + 103, 0xFF555555, false);
    }

    private void renderSortingCachePanel(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.fill(x + 78, y + 28, x + 170, y + 107, 0xFFC6C6C6);
        graphics.outline(x + 78, y + 28, 92, 79, 0xFF555555);
        renderSlot(graphics, SLOT, x + 82, y + 32);
        renderItem(graphics, sortingTargetStack(selectedSortingTarget), x + 82, y + 32);
        graphics.text(font, sortingTargetName(selectedSortingTarget), x + 104, y + 35, 0xFF404040, false);
        graphics.text(font, Component.literal("手動篩選"), x + 104, y + 46, 0xFF404040, false);
        renderSlot(graphics, SLOT, x + 82, y + 52);
        renderItem(graphics, new ItemStack(cacheValueIsTag ? Items.NAME_TAG : Items.DIAMOND), x + 82, y + 52);
        graphics.text(font, Component.literal(cacheValueIsTag ? "選取標籤" : "選取項目"), x + 104, y + 57, 0xFF555555, false);
        renderFilterDecisionRow(graphics, x + 82, y + 72, true, allowedCacheEntries[selectedSortingTarget]);
        renderFilterDecisionRow(graphics, x + 82, y + 90, false, deniedCacheEntries[selectedSortingTarget]);
        drawTextField(graphics, x + 10, y + 108, 156,
                Component.literal(cacheValueIsTag ? "#c:ores" : "minecraft:diamond"));
    }

    private ItemStack sortingTargetStack(int index) {
        return switch (index) {
            case 0 -> new ItemStack(Items.BARREL);
            case 1 -> new ItemStack(Items.CHEST);
            case 2 -> {
                var copperChest = BuiltInRegistries.ITEM.getValue(COPPER_CHEST_ID);
                yield new ItemStack(copperChest != null ? copperChest : Items.CHEST);
            }
            case 3 -> new ItemStack(Items.TRAPPED_CHEST);
            default -> new ItemStack(Items.ENDER_CHEST);
        };
    }

    private Component sortingTargetName(int index) {
        return switch (index) {
            case 0 -> Component.literal("木桶");
            case 1 -> Component.literal("箱子");
            case 2 -> Component.literal("銅箱");
            case 3 -> Component.literal("陷阱箱");
            default -> Component.literal("終界箱");
        };
    }

    private void renderArrowHead(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.horizontalLine(x, x, y - 4, 0xFF555555);
        graphics.horizontalLine(x, x + 2, y - 3, 0xFF555555);
        graphics.horizontalLine(x, x + 3, y - 2, 0xFF555555);
        graphics.horizontalLine(x, x + 4, y - 1, 0xFF555555);
        graphics.horizontalLine(x, x + 5, y, 0xFF555555);
        graphics.horizontalLine(x, x + 4, y + 1, 0xFF555555);
        graphics.horizontalLine(x, x + 3, y + 2, 0xFF555555);
        graphics.horizontalLine(x, x + 2, y + 3, 0xFF555555);
        graphics.horizontalLine(x, x, y + 4, 0xFF555555);
    }

    private void renderIconAction(GuiGraphicsExtractor graphics, ItemStack icon, int x, int y) {
        graphics.fill(x + 2, y + 2, x + 20, y + 20, 0x70000000);
        graphics.fill(x, y, x + 18, y + 18, 0x99303030);
        graphics.outline(x, y, 18, 18, 0xFF161616);
        renderItem(graphics, icon, x + 1, y + 1);
    }

    private void renderFilterDecisionRow(GuiGraphicsExtractor graphics, int x, int y, boolean allowed, int count) {
        int color = allowed ? 0xFF50B850 : 0xFFD04A4A;
        graphics.fill(x, y, x + 84, y + 16, allowed ? 0xFF4F6D4F : 0xFF704E4E);
        graphics.outline(x, y, 84, 16, 0xFF303030);
        if (allowed) {
            graphics.horizontalLine(x + 5, x + 17, y + 8, color);
            graphics.horizontalLine(x + 12, x + 16, y + 5, color);
            graphics.horizontalLine(x + 12, x + 17, y + 6, color);
            graphics.horizontalLine(x + 12, x + 18, y + 7, color);
            graphics.horizontalLine(x + 12, x + 18, y + 9, color);
            graphics.horizontalLine(x + 12, x + 17, y + 10, color);
            graphics.horizontalLine(x + 12, x + 16, y + 11, color);
        } else {
            graphics.horizontalLine(x + 5, x + 17, y + 8, color);
            graphics.horizontalLine(x + 6, x + 10, y + 5, color);
            graphics.horizontalLine(x + 5, x + 10, y + 6, color);
            graphics.horizontalLine(x + 4, x + 10, y + 7, color);
            graphics.horizontalLine(x + 4, x + 10, y + 9, color);
            graphics.horizontalLine(x + 5, x + 10, y + 10, color);
            graphics.horizontalLine(x + 6, x + 10, y + 11, color);
        }
        graphics.text(font, Component.literal((allowed ? "加入允許" : "加入拒絕") + "  " + count),
                x + 24, y + 4, 0xFFFFFFFF, false);
    }

    private void renderIconTooltip(GuiGraphicsExtractor graphics, int x, int y, int mouseX, int mouseY) {
        if (inBounds(mouseX, mouseY, x + 104, y + 3, 18, 18)) {
            graphics.setTooltipForNextFrame(font, Component.literal(stopped ? "繼續採集" : "停止採集"), mouseX, mouseY);
        } else if (inBounds(mouseX, mouseY, x + 128, y + 3, 18, 18)) {
            graphics.setTooltipForNextFrame(font, Component.literal(sortingMode ? "切換為採集模式" : "切換為整理模式"), mouseX, mouseY);
        } else if (inBounds(mouseX, mouseY, x + 152, y + 3, 18, 18)) {
            graphics.setTooltipForNextFrame(font, Component.literal(llmPanelVisible ? "返回魁儡狀態" : "LLM 設定"), mouseX, mouseY);
        }
    }

    private void renderSortingTargetTooltip(GuiGraphicsExtractor graphics, int x, int y, int mouseX, int mouseY) {
        if (!sortingCachePanelVisible) {
            int targetIndex = sortingTargetAt(mouseX, mouseY, x, y);
            if (targetIndex >= 0) {
                graphics.setTooltipForNextFrame(font,
                        Component.literal(sortingTargetName(targetIndex).getString() + "：管理手動快取"), mouseX, mouseY);
            }
        }
    }

    private void renderSortingCacheTooltip(GuiGraphicsExtractor graphics, int x, int y, int mouseX, int mouseY) {
        if (sortingCachePanelVisible && inBounds(mouseX, mouseY, x + 82, y + 52, 18, 18)) {
            graphics.setTooltipForNextFrame(font, Component.literal(cacheValueIsTag ? "快取類型：標籤" : "快取類型：物品"), mouseX, mouseY);
        } else if (sortingCachePanelVisible && inBounds(mouseX, mouseY, x + 82, y + 72, 84, 16)) {
            graphics.setTooltipForNextFrame(font, Component.literal("將選取項目加入允許快取"), mouseX, mouseY);
        } else if (sortingCachePanelVisible && inBounds(mouseX, mouseY, x + 82, y + 90, 84, 16)) {
            graphics.setTooltipForNextFrame(font, Component.literal("將選取項目加入拒絕快取"), mouseX, mouseY);
        }
    }

    private void renderLlmPanel(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.text(font, Component.literal("LLM 設定"), x + 90, y + 33, 0xFF404040, false);
        renderIconAction(graphics, new ItemStack(Items.LEVER), x + 143, y + 31);
        graphics.text(font, Component.literal(llmEnabled ? "● 已啟用" : "● 已停用"), x + 96, y + 48,
                llmEnabled ? 0xFF287C35 : 0xFF9B3030, false);
        drawTextField(graphics, x + 89, y + 61, 75, Component.literal("gpt-5"));
        graphics.text(font, Component.literal("提示詞"), x + 90, y + 84, 0xFF404040, false);
        drawTextField(graphics, x + 89, y + 95, 75, Component.literal("選擇採掘目標"));
    }

    private void renderLlmToggleTooltip(GuiGraphicsExtractor graphics, int x, int y, int mouseX, int mouseY) {
        if (inBounds(mouseX, mouseY, x + 143, y + 31, 18, 18)) {
            graphics.setTooltipForNextFrame(font, Component.literal(llmEnabled ? "停用 LLM" : "啟用 LLM"), mouseX, mouseY);
        }
    }

    private void drawTextField(GuiGraphicsExtractor graphics, int x, int y, int width, Component value) {
        graphics.fill(x, y, x + width, y + 18, 0xFF000000);
        graphics.fill(x + 1, y + 1, x + width - 1, y + 17, 0xFF505050);
        graphics.fill(x + 2, y + 2, x + width - 2, y + 16, 0xFF202020);
        graphics.text(font, value, x + 4, y + 5, 0xFFE0E0E0, false);
    }
}
