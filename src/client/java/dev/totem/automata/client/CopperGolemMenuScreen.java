package dev.totem.automata.client;

import dev.totem.automata.menu.CopperGolemMenu;
import dev.totem.automata.menu.CopperGolemMenuLayout;
import dev.totem.automata.mixin.client.SlotAccessor;
import dev.totem.automata.network.CopperGolemGatheringTargetPayload;
import dev.totem.core.api.v1.client.observer.ObserverReadOnlyScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.PreeditEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.UUID;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Automata-owned Copper Golem renderer. It already consumes the authoritative
 * snapshot; the remaining legacy card/editor parity keeps registration
 * cutover-only for now.
 */
public final class CopperGolemMenuScreen extends AbstractContainerScreen<CopperGolemMenu>
        implements ObserverReadOnlyScreen {
    private static final int INFO_ICON_SIZE = 20;
    private static final int INFO_ICON_STRIDE = 24;
    private static final int MAX_VISIBLE_SORTING_TARGETS = 5;
    private static final int FILTER_GRID_COLUMNS = 4;
    private static final int MAX_VISIBLE_FILTER_ENTRIES = FILTER_GRID_COLUMNS * 2;
    private static final Identifier FURNACE_TEXTURE = Identifier.withDefaultNamespace("textures/gui/container/furnace.png");
    private static final Identifier LIT_PROGRESS = Identifier.withDefaultNamespace("container/furnace/lit_progress");
    private static final Identifier SLOT = Identifier.withDefaultNamespace("container/slot");
    private static final Identifier TOOL_SLOT = Identifier.withDefaultNamespace("container/slot/pickaxe");

    private final CopperGolemMenuScreenLifecycle lifecycle;
    private final CopperGolemMenuUiState ui = new CopperGolemMenuUiState();
    private EditBox apiUrlField;
    private EditBox apiKeyField;
    private EditBox modelField;
    private EditBox gatheringPromptField;
    private EditBox bindingPromptField;
    private EditBox cacheValueField;
    private UUID editorGolem;
    private int editorRevision = Integer.MIN_VALUE;
    private int bindingEditorIndex = -1;
    private int bindingEditorRevision = Integer.MIN_VALUE;
    private int gatheringTargetScroll;
    private boolean cacheValueIsTag;
    private boolean bindingDetailVisible;
    private boolean filterTextEntryVisible;
    private boolean filterTextEntryAllowed;
    private ItemStack draggedFilterItem = ItemStack.EMPTY;
    private boolean targetBlocksVisible;
    private CopperGolem previewGolem;
    private final boolean observerReadOnly;
    private final Runnable observerStop;

    public CopperGolemMenuScreen(CopperGolemMenu menu, Inventory inventory, Component title) {
        this(menu, inventory, title, false);
    }

    /** Creates the production Automata screen without target action authority. */
    public CopperGolemMenuScreen(CopperGolemMenu menu, Inventory inventory, Component title,
                                 boolean observerReadOnly) {
        this(menu, inventory, title, observerReadOnly, () -> { });
    }

    public CopperGolemMenuScreen(CopperGolemMenu menu, Inventory inventory, Component title,
                                 boolean observerReadOnly, Runnable observerStop) {
        super(menu, inventory, title, CopperGolemMenuPanelLayout.PREFERRED_WIDTH, CopperGolemMenuPanelLayout.PREFERRED_HEIGHT);
        lifecycle = new CopperGolemMenuScreenLifecycle(menu, inventory, title);
        this.observerReadOnly = observerReadOnly;
        this.observerStop = observerStop;
    }

    @Override public boolean totem$isObserverReadOnly() { return observerReadOnly; }

    @Override public void onClose() {
        if (observerReadOnly) observerStop.run();
        else super.onClose();
    }

    @Override
    protected void init() {
        super.init();
        if (!observerReadOnly) lifecycle.open();
        var bounds = CopperGolemMenuPanelLayout.bounds(width, height);
        leftPos = bounds.x();
        topPos = bounds.y();
        updateMenuSlotLayout(bounds);
        positionInventoryLabel(bounds);
        int editorX = bounds.x() + 10;
        int editorY = bounds.y() + 50;
        apiUrlField = addRenderableWidget(editor(Component.translatable("message.deadrecall.copper_wrench.llm_api_url"), editorX, editorY, 156, 2048,
                Component.literal("https://api.openai.com/v1/chat/completions")));
        apiKeyField = addRenderableWidget(editor(Component.translatable("message.deadrecall.copper_wrench.llm_api_key"), editorX, editorY + 18, 156, 512,
                Component.literal("sk-…")));
        modelField = addRenderableWidget(editor(Component.translatable("message.deadrecall.copper_wrench.llm_model"), editorX, editorY + 36, 156, 256,
                Component.literal("gpt-4o-mini")));
        gatheringPromptField = addRenderableWidget(editor(Component.translatable("message.deadrecall.copper_wrench.gathering_llm_prompt"), editorX, editorY + 54, 156, 2048,
                Component.translatable("message.deadrecall.copper_wrench.prompt_hint")));
        bindingPromptField = addRenderableWidget(editor(Component.translatable("message.deadrecall.copper_wrench.binding_llm_prompt"), editorX, editorY + 54, 156, 2048,
                Component.translatable("message.deadrecall.copper_wrench.binding_prompt_hint")));
        cacheValueField = addRenderableWidget(editor(Component.translatable("message.deadrecall.copper_wrench.cache_value"), editorX, bounds.y() + 108, 156, 256,
                Component.translatable("message.deadrecall.copper_wrench.ui_filter_value_hint")));
        if (observerReadOnly) {
            for (EditBox field : List.of(apiUrlField, apiKeyField, modelField,
                    gatheringPromptField, bindingPromptField, cacheValueField)) {
                field.setValue("");
                field.active = false;
            }
        }
        createPreviewGolem();
        updateEditorVisibility();
    }

    private void createPreviewGolem() {
        if (Minecraft.getInstance().level == null) return;
        var type = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.fromNamespaceAndPath("minecraft", "copper_golem"));
        if (type == null) return;
        Entity entity = type.create(Minecraft.getInstance().level, EntitySpawnReason.COMMAND);
        if (entity instanceof CopperGolem golem) {
            golem.setId(1_000_000);
            previewGolem = golem;
        }
    }

    private void refreshSnapshotState() {
        if (observerReadOnly) return;
        lifecycle.session().controller().snapshot().ifPresent(snapshot -> {
            menu.setGatheringSlotsVisible("gathering".equals(snapshot.mode()));
            if (menu.slots.size() > CopperGolemMenuLayout.SLOT_FUEL) {
                boolean sorting = "sorting".equals(snapshot.mode());
                boolean editingFilter = sorting && bindingDetailVisible;
                setSlotPosition(menu.slots.get(CopperGolemMenuLayout.SLOT_FUEL),
                        editingFilter ? -1000 : sorting ? 8 : 119, editingFilter ? -1000 : sorting ? 99 : 71);
            }
            if (!snapshot.golemId().equals(editorGolem) || snapshot.revision() != editorRevision) {
                apiUrlField.setValue(snapshot.llmApiUrl());
                apiKeyField.setValue(snapshot.llmApiKey());
                modelField.setValue(snapshot.llmModel());
                gatheringPromptField.setValue(snapshot.gatheringLlmPrompt());
                editorGolem = snapshot.golemId();
                editorRevision = snapshot.revision();
            }
            refreshBindingEditor(snapshot);
        });
        updateEditorVisibility();
    }

    @Override
    public void removed() {
        draggedFilterItem = ItemStack.EMPTY;
        if (!observerReadOnly) lifecycle.close();
        super.removed();
    }

    /** Supplies deterministic, non-secret state to the client visual GameTest. */
    void acceptSnapshotForVisualTest(dev.totem.automata.network.CopperWrenchBindingsPayload snapshot) {
        lifecycle.session().accept(snapshot);
    }

    /** Applies an already-redacted owner snapshot without issuing a request. */
    public void acceptObserverSnapshot(dev.totem.automata.network.CopperWrenchBindingsPayload snapshot) {
        if (!observerReadOnly) throw new IllegalStateException("Not an Observer screen");
        lifecycle.session().accept(snapshot);
        menu.setGatheringSlotsVisible("gathering".equals(snapshot.mode()));
    }

    /** Returns current target state for the owner provider; caller must redact it. */
    public java.util.Optional<dev.totem.automata.network.CopperWrenchBindingsPayload> observerCaptureSource() {
        return lifecycle.session().controller().snapshot();
    }

    /** Selects a binding without sending input, solely for the deterministic visual GameTest. */
    void selectBindingForVisualTest(int index) {
        lifecycle.session().controller().snapshot().ifPresent(snapshot -> ui.select(index, snapshot.bindings().size()));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (observerReadOnly) return true;
        var snapshot = lifecycle.session().controller().snapshot().orElse(null);
        var bounds = CopperGolemMenuPanelLayout.bounds(width, height);
        if (event.button() == 0 && isInside(event.x(), event.y(), bounds.x() + 104, bounds.y() + 3, 18, 18)) {
            lifecycle.session().toggleOperation();
            return true;
        }
        if (event.button() == 0 && isInside(event.x(), event.y(), bounds.x() + 128, bounds.y() + 3, 18, 18)) {
            lifecycle.session().switchMode();
            bindingDetailVisible = false;
            filterTextEntryVisible = false;
            return true;
        }
        if (event.button() == 0 && isInside(event.x(), event.y(), bounds.x() + 152, bounds.y() + 3, 18, 18)) {
            selectTab(ui.tab() == CopperGolemMenuUiState.Tab.LLM
                    ? CopperGolemMenuUiState.Tab.BINDINGS : CopperGolemMenuUiState.Tab.LLM);
            return true;
        }
        if (snapshot == null) return super.mouseClicked(event, doubleClick);
        if (ui.tab() == CopperGolemMenuUiState.Tab.LLM && event.button() == 0) {
            if (isInside(event.x(), event.y(), bounds.x() + 95, bounds.y() + 31, 18, 18)) {
                lifecycle.session().testApiConnection(apiUrlField.getValue(), apiKeyField.getValue(), modelField.getValue());
                return true;
            }
            if (isInside(event.x(), event.y(), bounds.x() + 119, bounds.y() + 31, 18, 18)) {
                saveLlmPanel();
                return true;
            }
            if (isInside(event.x(), event.y(), bounds.x() + 143, bounds.y() + 31, 18, 18)) {
                toggleVisibleLlm();
                return true;
            }
        }
        if (ui.tab() == CopperGolemMenuUiState.Tab.BINDINGS && "sorting".equals(snapshot.mode())) {
            if (bindingDetailVisible && event.button() == 0) {
                if (filterTextEntryVisible) {
                    if (isInside(event.x(), event.y(), bounds.x() + 132, bounds.y() + 34, 36, 12)) {
                        cacheValueIsTag = !cacheValueIsTag;
                        if (cacheValueIsTag) cacheValueField.setValue("");
                        updateEditorVisibility();
                        return true;
                    }
                    if (isInside(event.x(), event.y(), bounds.x() + 10, bounds.y() + 82, 72, 18)) {
                        finishFilterTextEntry();
                        return true;
                    }
                    return super.mouseClicked(event, doubleClick);
                }
                ItemStack selectedItem = playerInventoryItemAt(event.x(), event.y());
                if (!selectedItem.isEmpty()) {
                    draggedFilterItem = selectedItem.copyWithCount(1);
                    return true;
                }
                if (isInside(event.x(), event.y(), bounds.x() + 8, bounds.y() + 29, 18, 18)) {
                    bindingDetailVisible = false;
                    updateEditorVisibility();
                    return true;
                }
                if (isInside(event.x(), event.y(), bounds.x() + 132, bounds.y() + 34, 36, 12)) {
                    cacheValueIsTag = !cacheValueIsTag;
                    if (cacheValueIsTag) cacheValueField.setValue("");
                    updateEditorVisibility();
                    return true;
                }
                FilterEntry entry = filterEntryAt(event.x(), event.y(), bounds, snapshot);
                if (entry != null) {
                    moveCachedDecision(entry, !entry.allowed());
                    return true;
                }
                if (isInside(event.x(), event.y(), bounds.x() + 8, bounds.y() + 54, 18, 18)) {
                    openFilterTextEntry(true);
                    return true;
                }
                if (isInside(event.x(), event.y(), bounds.x() + 94, bounds.y() + 54, 18, 18)) {
                    openFilterTextEntry(false);
                    return true;
                }
            }
            int index = bindingIndexAt(event.x(), event.y(), snapshot.bindings().size());
            if (index >= 0) {
                ui.select(index, snapshot.bindings().size());
                bindingEditorIndex = -1;
                bindingDetailVisible = true;
                filterTextEntryVisible = false;
                updateEditorVisibility();
                return true;
            }
        }
        if (ui.tab() == CopperGolemMenuUiState.Tab.BINDINGS && snapshot != null && "gathering".equals(snapshot.mode())
                && event.button() == 0 && inTargetBubble(event.x(), event.y(), bounds)) {
            targetBlocksVisible = !targetBlocksVisible;
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (observerReadOnly) return true;
        if (event.button() == 0 && !draggedFilterItem.isEmpty()) return true;
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (observerReadOnly) return true;
        if (event.button() == 0 && !draggedFilterItem.isEmpty()) {
            Boolean allowed = filterDropTargetAt(event.x(), event.y());
            if (allowed != null) {
                moveCachedDecision(new FilterEntry(BuiltInRegistries.ITEM.getKey(draggedFilterItem.getItem()).toString(), false, allowed), allowed);
            }
            draggedFilterItem = ItemStack.EMPTY;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (observerReadOnly) return true;
        var snapshot = lifecycle.session().controller().snapshot().orElse(null);
        if (ui.tab() != CopperGolemMenuUiState.Tab.BINDINGS || snapshot == null || verticalAmount == 0) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
        var bounds = CopperGolemMenuPanelLayout.bounds(width, height);
        if ("sorting".equals(snapshot.mode())) {
            int max = Math.max(0, snapshot.bindings().size() - MAX_VISIBLE_SORTING_TARGETS);
            ui.scroll(ui.scroll() + (verticalAmount < 0 ? 1 : -1), max);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (!observerReadOnly) return super.keyPressed(event);
        if (event.key() == 256) onClose();
        return true;
    }

    @Override public boolean charTyped(CharacterEvent event) {
        return observerReadOnly || super.charTyped(event);
    }

    @Override public boolean preeditUpdated(PreeditEvent event) {
        return observerReadOnly || super.preeditUpdated(event);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tick) {
        graphics.fill(0, 0, width, height, 0xC0000000);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tick) {
        refreshSnapshotState();
        var bounds = CopperGolemMenuPanelLayout.bounds(width, height);
        drawCustomTop(graphics, bounds);
        graphics.blit(RenderPipelines.GUI_TEXTURED, FURNACE_TEXTURE, bounds.x(), bounds.y() + 126, 0.0F, 70.0F,
                bounds.width(), bounds.height() - 126, 256, 256);
        graphics.text(font, Component.literal(trimToWidth(Component.translatable("entity.minecraft.copper_golem").getString(), 88)),
                bounds.x() + 8, bounds.y() + 9, 0xFF404040, false);
        lifecycle.session().controller().snapshot().ifPresentOrElse(
                snapshot -> drawSnapshot(graphics, bounds, snapshot, mouseX, mouseY),
                () -> graphics.text(font, Component.translatable("message.deadrecall.copper_wrench.ui_waiting_state"),
                        bounds.x() + 12, bounds.y() + 58, 0xFF9B3030));
        drawHeaderIcons(graphics, bounds, mouseX, mouseY);
        drawSlotBackings(graphics);
        super.extractRenderState(graphics, mouseX, mouseY, tick);
        if (!draggedFilterItem.isEmpty()) {
            graphics.item(draggedFilterItem, mouseX - 8, mouseY - 8);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        // The vanilla lower inventory texture already communicates this area.
    }

    private void drawSnapshot(GuiGraphicsExtractor graphics, CopperGolemMenuPanelLayout.Bounds bounds,
                              dev.totem.automata.network.CopperWrenchBindingsPayload snapshot,
                              int mouseX, int mouseY) {
        if (ui.tab() == CopperGolemMenuUiState.Tab.LLM) {
            drawLlmPanel(graphics, bounds, snapshot, mouseX, mouseY);
            return;
        }
        drawActivity(graphics, bounds, snapshot);
        if ("sorting".equals(snapshot.mode())) {
            drawBindingTab(graphics, bounds, snapshot, mouseX, mouseY);
            return;
        }
        drawGatheringTab(graphics, bounds, snapshot, mouseX, mouseY);
    }

    private void drawBindingTab(GuiGraphicsExtractor graphics, CopperGolemMenuPanelLayout.Bounds bounds,
                                dev.totem.automata.network.CopperWrenchBindingsPayload snapshot,
                                int mouseX, int mouseY) {
        if (filterTextEntryVisible) {
            drawBindingFilterTextEntry(graphics, bounds, mouseX, mouseY);
        } else if (bindingDetailVisible) {
            drawBindingFilterDetail(graphics, bounds, snapshot, mouseX, mouseY);
        } else {
            drawBindingFlow(graphics, bounds, snapshot, mouseX, mouseY);
        }
    }

    private void drawGatheringTab(GuiGraphicsExtractor graphics, CopperGolemMenuPanelLayout.Bounds bounds,
                                  dev.totem.automata.network.CopperWrenchBindingsPayload snapshot,
                                  int mouseX, int mouseY) {
        drawGatheringThoughtBubble(graphics, bounds, snapshot, mouseX, mouseY);
        drawGatheringHome(graphics, bounds, snapshot, mouseX, mouseY);
        renderFuelRemaining(graphics, bounds, snapshot, false, mouseX, mouseY);
    }

    private void drawInfoIcon(GuiGraphicsExtractor graphics, ItemStack stack, int x, int y, int borderColor,
                              int mouseX, int mouseY, List<Component> tooltip) {
        graphics.fill(x, y, x + INFO_ICON_SIZE, y + INFO_ICON_SIZE, 0xB0000000);
        graphics.outline(x, y, INFO_ICON_SIZE, INFO_ICON_SIZE, borderColor);
        graphics.item(stack, x + 2, y + 2);
        if (isInside(mouseX, mouseY, x, y, INFO_ICON_SIZE, INFO_ICON_SIZE)) {
            graphics.setComponentTooltipForNextFrame(font, tooltip, mouseX, mouseY);
        }
    }

    private void drawCustomTop(GuiGraphicsExtractor graphics, CopperGolemMenuPanelLayout.Bounds bounds) {
        int x = bounds.x();
        int y = bounds.y();
        graphics.fill(x, y, x + bounds.width(), y + 126, 0xFFC6C6C6);
        graphics.fill(x + 2, y + 2, x + bounds.width() - 2, y + 3, 0xFFFFFFFF);
        graphics.fill(x + 2, y + 124, x + bounds.width() - 2, y + 126, 0xFF555555);
        graphics.verticalLine(x, y, y + 125, 0xFF000000);
        graphics.verticalLine(x + bounds.width() - 1, y, y + 125, 0xFF000000);
        graphics.horizontalLine(x, x + bounds.width() - 1, y, 0xFF000000);
    }

    private void drawHeaderIcons(GuiGraphicsExtractor graphics, CopperGolemMenuPanelLayout.Bounds bounds, int mouseX, int mouseY) {
        int x = bounds.x();
        int y = bounds.y();
        var snapshot = lifecycle.session().controller().snapshot().orElse(null);
        renderIconAction(graphics, new ItemStack(Items.BARRIER), x + 104, y + 3);
        renderIconAction(graphics, new ItemStack(Items.COMPASS), x + 128, y + 3);
        renderIconAction(graphics, new ItemStack(Items.WRITABLE_BOOK), x + 152, y + 3);
        if (isInside(mouseX, mouseY, x + 104, y + 3, 18, 18)) {
            graphics.setTooltipForNextFrame(font, operationText(), mouseX, mouseY);
        } else if (isInside(mouseX, mouseY, x + 128, y + 3, 18, 18)) {
            graphics.setTooltipForNextFrame(font, modeText(), mouseX, mouseY);
        } else if (isInside(mouseX, mouseY, x + 152, y + 3, 18, 18)) {
            graphics.setTooltipForNextFrame(font, Component.translatable(ui.tab() == CopperGolemMenuUiState.Tab.LLM
                    ? "message.deadrecall.copper_wrench.ui_back_to_golem"
                    : "message.deadrecall.copper_wrench.ui_llm_settings"), mouseX, mouseY);
        }
        if (snapshot == null) return;
    }

    private void drawActivity(GuiGraphicsExtractor graphics, CopperGolemMenuPanelLayout.Bounds bounds,
                              dev.totem.automata.network.CopperWrenchBindingsPayload snapshot) {
        if (bindingDetailVisible) return;
        int color = !snapshot.running() ? 0xFF9B3030
                : snapshot.activity().startsWith("blocked_") ? 0xFFAA7A20 : 0xFF287C35;
        graphics.text(font, Component.literal(snapshot.running() ? "● " : "○ ")
                        .append(Component.translatable("message.deadrecall.copper_wrench.activity_" + snapshot.activity())),
                bounds.x() + 92, bounds.y() + 29, color, false);
    }

    private void drawBindingFlow(GuiGraphicsExtractor graphics, CopperGolemMenuPanelLayout.Bounds bounds,
                                 dev.totem.automata.network.CopperWrenchBindingsPayload snapshot,
                                 int mouseX, int mouseY) {
        int x = bounds.x();
        int y = bounds.y();
        int sourceX = x + 12;
        int sourceY = y + 70;
        int targetX = x + 146;
        int trunkX = x + 133;
        int flowY = sourceY + 9;
        int visible = Math.min(MAX_VISIBLE_SORTING_TARGETS, Math.max(0, snapshot.bindings().size() - ui.scroll()));
        if (ui.selected() < 0 && !snapshot.bindings().isEmpty()) ui.select(0, snapshot.bindings().size());
        graphics.text(font, Component.translatable("message.deadrecall.copper_wrench.ui_sorting_step_source"), sourceX, y + 58, 0xFF555555, false);
        graphics.text(font, Component.translatable("message.deadrecall.copper_wrench.ui_sorting_step_golem"), x + 64, y + 58, 0xFF555555, false);
        Component targetStep = Component.translatable("message.deadrecall.copper_wrench.ui_sorting_step_target");
        graphics.text(font, targetStep, x + bounds.width() - 12 - font.width(targetStep),
                y + 58, 0xFF555555, false);
        renderSlot(graphics, SLOT, sourceX, sourceY);
        var source = snapshot.sourceContainer();
        renderItem(graphics, source == null ? new ItemStack(Items.CHEST) : iconStack(source.itemId()), sourceX, sourceY);
        if (isInside(mouseX, mouseY, sourceX, sourceY, 18, 18)) {
            graphics.setComponentTooltipForNextFrame(font, sourceTooltip(source), mouseX, mouseY);
        }
        graphics.horizontalLine(sourceX + 20, x + 49, flowY, 0xFF555555);
        renderArrowHead(graphics, x + 45, flowY);
        graphics.horizontalLine(x + 116, trunkX, flowY, 0xFF555555);
        if (visible > 0) {
            graphics.verticalLine(trunkX, sortingTargetY(y, visible, 0) + 9,
                    sortingTargetY(y, visible, visible - 1) + 9, 0xFF555555);
        }
        for (int cell = 0; cell < visible; cell++) {
            int index = ui.scroll() + cell;
            var binding = snapshot.bindings().get(index);
            int targetY = sortingTargetY(y, visible, cell);
            int targetFlowY = targetY + 9;
            renderSlot(graphics, SLOT, targetX, targetY);
            renderItem(graphics, iconStack(binding.itemId()), targetX, targetY);
            if (index == ui.selected()) graphics.outline(targetX - 1, targetY - 1, 20, 20, 0xFFE0C24D);
            graphics.horizontalLine(trunkX, targetX - 3, targetFlowY, 0xFF555555);
            renderArrowHead(graphics, targetX - 7, targetFlowY);
            if (isInside(mouseX, mouseY, targetX, targetY, 18, 18)) {
                graphics.setComponentTooltipForNextFrame(font, bindingTooltip(binding, index), mouseX, mouseY);
            }
        }
        Component sortingHint = Component.translatable(snapshot.bindings().isEmpty()
                ? "message.deadrecall.copper_wrench.ui_sorting_bind_targets_short"
                : "message.deadrecall.copper_wrench.ui_sorting_edit_target_short");
        graphics.text(font, sortingHint, x + 104, y + 103, 0xFF3F6F9F, false);
        graphics.text(font,
                Component.translatable("message.deadrecall.copper_wrench.ui_binding_count_short",
                        snapshot.bindings().size()),
                x + 104, y + 114, 0xFF555555, false);
        renderPreviewGolem(graphics, bounds, snapshot, mouseX, mouseY,
                x + 50, y + 67, x + 124, y + 101);
        renderFuelRemaining(graphics, bounds, snapshot, true, mouseX, mouseY);
    }

    private void drawBindingFilterDetail(GuiGraphicsExtractor graphics, CopperGolemMenuPanelLayout.Bounds bounds,
                                         dev.totem.automata.network.CopperWrenchBindingsPayload snapshot,
                                         int mouseX, int mouseY) {
        var binding = selectedBinding(snapshot).orElse(null);
        if (binding == null) {
            bindingDetailVisible = false;
            updateEditorVisibility();
            drawBindingFlow(graphics, bounds, snapshot, mouseX, mouseY);
            return;
        }
        int x = bounds.x();
        int y = bounds.y();
        renderSlot(graphics, SLOT, x + 8, y + 29);
        renderItem(graphics, new ItemStack(Items.BARREL), x + 8, y + 29);
        graphics.text(font, Component.literal(trimToWidth(blockDisplayName(binding.blockId()).getString(), 84)),
                x + 30, y + 32, 0xFF404040, false);
        graphics.text(font, Component.translatable("message.deadrecall.copper_wrench.ui_manual_filter"),
                x + 30, y + 43, 0xFF555555, false);
        graphics.text(font, Component.translatable(cacheValueIsTag
                ? "message.deadrecall.copper_wrench.ui_filter_mode_tag"
                : "message.deadrecall.copper_wrench.ui_filter_mode_item"), x + 132, y + 36, 0xFF3F6F9F, false);
        drawFilterPane(graphics, x + 8, y + 54, binding, true, mouseX, mouseY);
        drawFilterPane(graphics, x + 94, y + 54, binding, false, mouseX, mouseY);
        if (isInside(mouseX, mouseY, x + 8, y + 29, 18, 18)) {
            graphics.setTooltipForNextFrame(font, Component.translatable("message.deadrecall.copper_wrench.ui_back_to_targets"), mouseX, mouseY);
        } else if (isInside(mouseX, mouseY, x + 132, y + 34, 36, 12)) {
            graphics.setTooltipForNextFrame(font, Component.translatable(cacheValueIsTag
                    ? "message.deadrecall.copper_wrench.entry_type_tag"
                    : "message.deadrecall.copper_wrench.ui_filter_item_picker_hint"), mouseX, mouseY);
        }
    }

    private void drawBindingFilterTextEntry(GuiGraphicsExtractor graphics, CopperGolemMenuPanelLayout.Bounds bounds,
                                            int mouseX, int mouseY) {
        int x = bounds.x();
        int y = bounds.y();
        boolean allowed = filterTextEntryAllowed;
        graphics.fill(x + 8, y + 29, x + 168, y + 105, 0xFFC6C6C6);
        graphics.outline(x + 8, y + 29, 160, 76, 0xFF555555);
        graphics.text(font, Component.translatable(allowed
                        ? "message.deadrecall.copper_wrench.ui_filter_add_allowed_title"
                        : "message.deadrecall.copper_wrench.ui_filter_add_denied_title"),
                x + 18, y + 38, allowed ? 0xFF287C35 : 0xFF9B3030, false);
        graphics.text(font, Component.translatable(cacheValueIsTag
                        ? "message.deadrecall.copper_wrench.ui_filter_mode_tag"
                        : "message.deadrecall.copper_wrench.ui_filter_mode_item"),
                x + 132, y + 36, 0xFF3F6F9F, false);
        renderFilterActionSlot(graphics, x + 10, y + 82, allowed);
        graphics.text(font, Component.translatable("message.deadrecall.copper_wrench.ui_filter_finish"),
                x + 34, y + 87, 0xFFFFFFFF, false);
        if (isInside(mouseX, mouseY, x + 132, y + 34, 36, 12)) {
            graphics.setTooltipForNextFrame(font, Component.translatable(cacheValueIsTag
                    ? "message.deadrecall.copper_wrench.entry_type_tag"
                    : "message.deadrecall.copper_wrench.entry_type_item"), mouseX, mouseY);
        } else if (isInside(mouseX, mouseY, x + 10, y + 82, 72, 18)) {
            graphics.setTooltipForNextFrame(font, Component.translatable("message.deadrecall.copper_wrench.ui_filter_finish"), mouseX, mouseY);
        }
    }

    private void drawFilterPane(GuiGraphicsExtractor graphics, int paneX, int paneY,
                                dev.totem.automata.network.CopperWrenchBindingsPayload.BindingEntry binding,
                                boolean allowed, int mouseX, int mouseY) {
        renderFilterActionSlot(graphics, paneX, paneY, allowed);
        graphics.text(font, Component.translatable(allowed
                        ? "message.deadrecall.copper_wrench.cache_side_accepted"
                        : "message.deadrecall.copper_wrench.cache_side_denied"),
                paneX + 22, paneY + 5, allowed ? 0xFF287C35 : 0xFF9B3030, false);
        if (isInside(mouseX, mouseY, paneX, paneY, 18, 18)) {
            graphics.setTooltipForNextFrame(font, Component.translatable(allowed
                    ? "message.deadrecall.copper_wrench.ui_add_allowed_tooltip"
                    : "message.deadrecall.copper_wrench.ui_add_denied_tooltip"), mouseX, mouseY);
        }
        List<FilterEntry> entries = filterEntries(binding, allowed);
        for (int index = 0; index < MAX_VISIBLE_FILTER_ENTRIES; index++) {
            int slotX = paneX + (index % FILTER_GRID_COLUMNS) * 18;
            int slotY = paneY + 20 + (index / FILTER_GRID_COLUMNS) * 18;
            renderSlot(graphics, SLOT, slotX, slotY);
            if (index >= entries.size()) continue;
            FilterEntry entry = entries.get(index);
            renderItem(graphics, filterEntryIcon(entry), slotX, slotY);
            if (isInside(mouseX, mouseY, slotX, slotY, 18, 18)) {
                graphics.setComponentTooltipForNextFrame(font, List.of(
                        Component.translatable(allowed
                                ? "message.deadrecall.copper_wrench.cache_side_accepted"
                                : "message.deadrecall.copper_wrench.cache_side_denied"),
                        Component.literal(entry.tag() ? "#" + entry.value() : entry.value()),
                        Component.translatable("message.deadrecall.copper_wrench.ui_filter_switch_side_hint")), mouseX, mouseY);
            }
        }
    }

    private void renderFilterActionSlot(GuiGraphicsExtractor graphics, int x, int y, boolean allowed) {
        graphics.fill(x, y, x + 18, y + 18, allowed ? 0xFF3E7540 : 0xFF873F3F);
        graphics.outline(x, y, 18, 18, 0xFF303030);
        if (allowed) {
            graphics.text(font, Component.literal("✓"), x + 5, y + 5, 0xFFFFFFFF, false);
        } else {
            renderItem(graphics, new ItemStack(Items.BARRIER), x, y);
        }
    }

    private List<FilterEntry> filterEntries(
            dev.totem.automata.network.CopperWrenchBindingsPayload.BindingEntry binding, boolean allowed) {
        List<FilterEntry> entries = new ArrayList<>();
        List<String> itemIds = allowed ? binding.llmAllowedItemIds() : binding.llmDeniedItemIds();
        List<String> tags = allowed ? binding.llmAllowedTags() : binding.llmDeniedTags();
        for (String itemId : itemIds) entries.add(new FilterEntry(itemId, false, allowed));
        for (String tag : tags) entries.add(new FilterEntry(tag, true, allowed));
        return entries;
    }

    private ItemStack filterEntryIcon(FilterEntry entry) {
        return entry.tag() ? new ItemStack(Items.NAME_TAG) : iconStack(entry.value());
    }

    private void drawGatheringThoughtBubble(GuiGraphicsExtractor graphics, CopperGolemMenuPanelLayout.Bounds bounds,
                                            dev.totem.automata.network.CopperWrenchBindingsPayload snapshot,
                                            int mouseX, int mouseY) {
        int x = bounds.x();
        int y = bounds.y();
        List<GatheringTarget> targets = gatheringAcceptedTargets(snapshot);
        if (targetBlocksVisible && !targets.isEmpty()) {
            graphics.fill(x + 33, y + 24, x + 87, y + 52, 0xFFFFFFFF);
            graphics.outline(x + 33, y + 24, 54, 28, 0xFF555555);
            for (int index = 0; index < Math.min(3, targets.size()); index++) {
                GatheringTarget target = targets.get(index);
                renderItem(graphics, target.tag() ? new ItemStack(Items.NAME_TAG) : iconStack(target.value()),
                        x + 36 + index * 17, y + 30);
            }
        } else {
            graphics.fill(x + 48, y + 34, x + 76, y + 52, 0xFFFFFFFF);
            graphics.outline(x + 48, y + 34, 28, 18, 0xFF555555);
            graphics.text(font, Component.literal("…"), x + 58, y + 38, 0xFF555555, false);
        }
        graphics.fill(x + 45, y + 51, x + 50, y + 56, 0xFFFFFFFF);
        graphics.fill(x + 41, y + 55, x + 44, y + 58, 0xFFFFFFFF);
        renderPreviewGolem(graphics, bounds, snapshot, mouseX, mouseY,
                x + 8, y + 24, x + 78, y + 118);
    }

    private void drawGatheringHome(GuiGraphicsExtractor graphics, CopperGolemMenuPanelLayout.Bounds bounds,
                                   dev.totem.automata.network.CopperWrenchBindingsPayload snapshot,
                                   int mouseX, int mouseY) {
        int x = bounds.x();
        int y = bounds.y();
        int chestX = x + 61;
        int chestY = y + 99;
        graphics.text(font, Component.translatable("message.deadrecall.copper_wrench.ui_home_short"), x + 57, y + 88, 0xFF555555, false);
        graphics.verticalLine(x + 40, y + 85, y + 108, 0xFF555555);
        graphics.horizontalLine(x + 40, x + 55, y + 108, 0xFF555555);
        renderArrowHead(graphics, x + 55, y + 108);
        renderSlot(graphics, SLOT, chestX, chestY);
        var source = snapshot.sourceContainer();
        renderItem(graphics, source == null ? new ItemStack(Items.CHEST) : iconStack(source.itemId()), chestX, chestY);
        if (isInside(mouseX, mouseY, chestX, chestY, 18, 18)) {
            graphics.setComponentTooltipForNextFrame(font, sourceTooltip(source), mouseX, mouseY);
        }
    }

    private void drawLlmPanel(GuiGraphicsExtractor graphics, CopperGolemMenuPanelLayout.Bounds bounds,
                              dev.totem.automata.network.CopperWrenchBindingsPayload snapshot,
                              int mouseX, int mouseY) {
        int x = bounds.x();
        int y = bounds.y();
        boolean enabled = visibleLlmEnabled(snapshot);
        graphics.text(font, Component.translatable("message.deadrecall.copper_wrench.ui_llm_settings"), x + 10, y + 33, 0xFF404040, false);
        renderIconAction(graphics, new ItemStack(Items.ENDER_EYE), x + 95, y + 31);
        renderIconAction(graphics, new ItemStack(Items.HOPPER), x + 119, y + 31);
        renderIconAction(graphics, new ItemStack(Items.LEVER), x + 143, y + 31);
        graphics.text(font, Component.translatable("message.deadrecall.copper_wrench.llm_state",
                        Component.translatable(enabled ? "message.deadrecall.copper_wrench.enabled"
                                : "message.deadrecall.copper_wrench.disabled")),
                x + 10, y + 39, enabled ? 0xFF287C35 : 0xFF9B3030, false);
        if (isInside(mouseX, mouseY, x + 95, y + 31, 18, 18)) {
            graphics.setTooltipForNextFrame(font, Component.translatable("message.deadrecall.copper_wrench.test_connection"), mouseX, mouseY);
        } else if (isInside(mouseX, mouseY, x + 119, y + 31, 18, 18)) {
            graphics.setTooltipForNextFrame(font, Component.translatable("message.deadrecall.copper_wrench.save"), mouseX, mouseY);
        } else if (isInside(mouseX, mouseY, x + 143, y + 31, 18, 18)) {
            graphics.setTooltipForNextFrame(font, Component.translatable(enabled
                    ? "message.deadrecall.copper_wrench.ui_disable_llm"
                    : "message.deadrecall.copper_wrench.ui_enable_llm"), mouseX, mouseY);
        }
    }

    private void renderPreviewGolem(GuiGraphicsExtractor graphics, CopperGolemMenuPanelLayout.Bounds bounds,
                                    dev.totem.automata.network.CopperWrenchBindingsPayload snapshot,
                                    int mouseX, int mouseY, int left, int top, int right, int bottom) {
        CopperGolem golem = null;
        if (Minecraft.getInstance().level != null) {
            Entity entity = Minecraft.getInstance().level.getEntity(snapshot.golemId());
            if (entity instanceof CopperGolem current) golem = current;
        }
        if (golem == null) golem = previewGolem;
        if (golem != null) {
            InventoryScreen.extractEntityInInventoryFollowsMouse(graphics, left, top, right, bottom,
                    30, 0.0625F, mouseX, mouseY, golem);
        }
    }

    private void renderFuelRemaining(GuiGraphicsExtractor graphics, CopperGolemMenuPanelLayout.Bounds bounds,
                                     dev.totem.automata.network.CopperWrenchBindingsPayload snapshot,
                                     boolean sorting, int mouseX, int mouseY) {
        if (!snapshot.infiniteFuel() && snapshot.fuelTicks() <= 0) return;
        int flameHeight = 10;
        int flameX = bounds.x() + (sorting ? 29 : 96);
        int flameY = bounds.y() + (sorting ? 102 : 76);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, LIT_PROGRESS, 14, 14, 0, 14 - flameHeight,
                flameX, flameY + 14 - flameHeight, 14, flameHeight);
        if (snapshot.infiniteFuel()) {
            graphics.text(font,
                    Component.translatable("message.deadrecall.copper_wrench.fuel_infinite_short"),
                    flameX + 8, flameY + 5, 0xFFFFFFFF, true);
            if (isInside(mouseX, mouseY, flameX, flameY, 18, 14)) {
                graphics.setTooltipForNextFrame(font,
                        Component.translatable("message.deadrecall.copper_wrench.fuel_infinite"),
                        mouseX, mouseY);
            }
        }
    }

    private void renderSlot(GuiGraphicsExtractor graphics, Identifier sprite, int x, int y) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, 18, 18);
    }

    private void renderItem(GuiGraphicsExtractor graphics, ItemStack stack, int x, int y) {
        graphics.item(stack, x + 1, y + 1);
        graphics.itemDecorations(font, stack, x + 1, y + 1);
    }

    private void renderIconAction(GuiGraphicsExtractor graphics, ItemStack icon, int x, int y) {
        graphics.fill(x + 2, y + 2, x + 20, y + 20, 0x70000000);
        graphics.fill(x, y, x + 18, y + 18, 0x99303030);
        graphics.outline(x, y, 18, 18, 0xFF161616);
        renderItem(graphics, icon, x, y);
    }

    private static void renderArrowHead(GuiGraphicsExtractor graphics, int x, int y) {
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

    private static int sortingTargetY(int panelY, int targetCount, int index) {
        return panelY + 70 - (targetCount - 1) * 9 + index * 18;
    }

    private boolean inTargetBubble(double mouseX, double mouseY, CopperGolemMenuPanelLayout.Bounds bounds) {
        return targetBlocksVisible
                ? isInside(mouseX, mouseY, bounds.x() + 33, bounds.y() + 24, 54, 28)
                : isInside(mouseX, mouseY, bounds.x() + 48, bounds.y() + 34, 28, 18);
    }

    private List<Component> sourceTooltip(
            dev.totem.automata.network.CopperWrenchBindingsPayload.BindingEntry source) {
        if (source == null) {
            return List.of(Component.translatable("message.deadrecall.copper_wrench.ui_source",
                    Component.translatable("message.deadrecall.copper_wrench.source_unbound")),
                    Component.translatable("message.deadrecall.copper_wrench.ui_sorting_source_setup_hint"));
        }
        return List.of(
                Component.translatable("message.deadrecall.copper_wrench.ui_source",
                        blockDisplayName(source.blockId())),
                Component.literal(source.blockId()),
                Component.translatable("message.deadrecall.copper_wrench.ui_container_location",
                        source.dimension(), source.x(), source.y(), source.z()),
                bindingStatusTooltip(source),
                Component.translatable("message.deadrecall.copper_wrench.ui_sorting_source_setup_hint")
        );
    }

    private List<Component> bindingTooltip(
            dev.totem.automata.network.CopperWrenchBindingsPayload.BindingEntry binding, int index) {
        Component llm = Component.translatable(binding.llmEnabled()
                ? "message.deadrecall.copper_wrench.llm_on"
                : "message.deadrecall.copper_wrench.llm_off");
        return List.of(
                Component.translatable("message.deadrecall.copper_wrench.target_container_number", index + 1)
                        .append(": ").append(blockDisplayName(binding.blockId())),
                Component.literal(binding.blockId()),
                Component.translatable("message.deadrecall.copper_wrench.ui_container_location",
                        binding.dimension(), binding.x(), binding.y(), binding.z()),
                bindingStatusTooltip(binding),
                Component.translatable("message.deadrecall.copper_wrench.llm_state", llm),
                Component.translatable("message.deadrecall.copper_wrench.cached_items_and_tags",
                        binding.llmCachedItemIds(), binding.llmCachedTags()),
                Component.translatable("message.deadrecall.copper_wrench.ui_sorting_target_edit_tooltip")
        );
    }

    private Component bindingStatusTooltip(
            dev.totem.automata.network.CopperWrenchBindingsPayload.BindingEntry binding) {
        String statusKey = !binding.loaded()
                ? "message.deadrecall.copper_wrench.binding_status_unloaded"
                : binding.available()
                ? "message.deadrecall.copper_wrench.binding_status_available"
                : "message.deadrecall.copper_wrench.binding_status_unavailable";
        return Component.translatable("message.deadrecall.copper_wrench.binding_status",
                Component.translatable(statusKey));
    }

    private List<Component> gatheringTargetTooltip(GatheringTarget target, Component group) {
        Component type = Component.translatable(target.tag()
                ? "message.deadrecall.copper_wrench.entry_type_tag"
                : "message.deadrecall.copper_wrench.entry_type_block");
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(Component.translatable("message.deadrecall.copper_wrench.target_tooltip_type", group, type));
        if (!target.tag()) tooltip.add(blockDisplayName(target.value()));
        tooltip.add(Component.literal(target.value()));
        tooltip.add(Component.translatable("message.deadrecall.copper_wrench.remove_icon_hint"));
        return tooltip;
    }

    private static boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private ItemStack operationStatusIcon(dev.totem.automata.network.CopperWrenchBindingsPayload snapshot) {
        if (!snapshot.running()) return new ItemStack(Items.BARRIER);
        return switch (snapshot.activity()) {
            case "blocked_no_fuel" -> new ItemStack(Items.COAL);
            case "blocked_no_tool", "blocked_tool_broken", "working" -> new ItemStack(Items.IRON_PICKAXE);
            case "blocked_no_home", "blocked_home_unavailable", "blocked_home_full", "depositing" -> new ItemStack(Items.CHEST);
            case "blocked_sorting" -> new ItemStack(Items.HOPPER);
            case "moving_to_target" -> new ItemStack(Items.MINECART);
            case "returning_home", "searching" -> new ItemStack(Items.COMPASS);
            case "idle" -> new ItemStack(Items.CLOCK);
            default -> new ItemStack(Items.EMERALD);
        };
    }

    private Component operationText() {
        return lifecycle.session().controller().snapshot()
                .map(snapshot -> Component.translatable(snapshot.running()
                        ? "message.deadrecall.copper_wrench.action_stop"
                        : "message.deadrecall.copper_wrench.action_start"))
                .orElse(Component.translatable("message.deadrecall.copper_wrench.ui_operation"));
    }

    private Component modeText() {
        return lifecycle.session().controller().snapshot()
                .map(snapshot -> Component.translatable("message.deadrecall.copper_wrench.mode_" + snapshot.mode()))
                .orElse(Component.translatable("message.deadrecall.copper_wrench.ui_mode"));
    }

    private EditBox editor(Component label, int x, int y, int width, int maxLength, Component hint) {
        EditBox field = new EditBox(font, x, y, width, 18, label);
        field.setMaxLength(maxLength);
        field.setHint(hint);
        return field;
    }

    private void selectTab(CopperGolemMenuUiState.Tab tab) {
        ui.tab(tab);
        if (tab == CopperGolemMenuUiState.Tab.LLM) {
            bindingDetailVisible = false;
            filterTextEntryVisible = false;
        }
        updateEditorVisibility();
    }

    private void saveLlmPanel() {
        lifecycle.session().saveApiConfig(apiUrlField.getValue(), apiKeyField.getValue(), modelField.getValue());
        lifecycle.session().controller().snapshot().ifPresent(snapshot -> {
            if ("sorting".equals(snapshot.mode())) {
                selectedBinding(snapshot).ifPresent(binding -> lifecycle.session().updateBindingLlm(
                        ui.selected(), binding.llmEnabled(), bindingPromptField.getValue()));
            } else {
                lifecycle.session().updateGatheringLlm(snapshot.gatheringLlmEnabled(), gatheringPromptField.getValue());
            }
        });
    }

    private boolean visibleLlmEnabled(dev.totem.automata.network.CopperWrenchBindingsPayload snapshot) {
        if ("sorting".equals(snapshot.mode())) {
            return selectedBinding(snapshot).map(dev.totem.automata.network.CopperWrenchBindingsPayload.BindingEntry::llmEnabled)
                    .orElse(false);
        }
        return snapshot.gatheringLlmEnabled();
    }

    private void toggleVisibleLlm() {
        lifecycle.session().controller().snapshot().ifPresent(snapshot -> {
            if ("sorting".equals(snapshot.mode())) {
                selectedBinding(snapshot).ifPresent(binding -> lifecycle.session().updateBindingLlm(
                        ui.selected(), !binding.llmEnabled(), bindingPromptField.getValue()));
            } else {
                lifecycle.session().updateGatheringLlm(!snapshot.gatheringLlmEnabled(), gatheringPromptField.getValue());
            }
        });
    }

    private void openFilterTextEntry(boolean allowed) {
        filterTextEntryAllowed = allowed;
        filterTextEntryVisible = true;
        cacheValueField.setFocused(true);
        updateEditorVisibility();
    }

    private void moveCachedDecision(FilterEntry entry, boolean allowed) {
        selectedBinding(lifecycle.session().controller().snapshot().orElse(null)).ifPresent(binding ->
                lifecycle.session().moveCachedDecision(ui.selected(), entry.value(), entry.tag(), allowed));
    }

    private void finishFilterTextEntry() {
        if (!cacheValueField.getValue().isBlank()) {
            moveCachedDecision(new FilterEntry(cacheValueField.getValue(), cacheValueIsTag, filterTextEntryAllowed),
                    filterTextEntryAllowed);
        }
        cacheValueField.setValue("");
        cacheValueField.setFocused(false);
        filterTextEntryVisible = false;
        updateEditorVisibility();
    }

    private void refreshBindingEditor(dev.totem.automata.network.CopperWrenchBindingsPayload snapshot) {
        selectedBinding(snapshot).ifPresentOrElse(binding -> {
            if (ui.selected() != bindingEditorIndex || snapshot.revision() != bindingEditorRevision) {
                bindingPromptField.setValue(binding.llmPrompt());
                bindingEditorIndex = ui.selected();
                bindingEditorRevision = snapshot.revision();
            }
        }, () -> bindingEditorIndex = -1);
    }

    private void updateEditorVisibility() {
        boolean llmTab = ui.tab() == CopperGolemMenuUiState.Tab.LLM;
        boolean gathering = lifecycle.session().controller().snapshot()
                .map(snapshot -> "gathering".equals(snapshot.mode())).orElse(false);
        setVisible(apiUrlField, llmTab);
        setVisible(apiKeyField, llmTab);
        setVisible(modelField, llmTab);
        setVisible(gatheringPromptField, llmTab && gathering);
        boolean bindingLlm = llmTab && !gathering
                && selectedBinding(lifecycle.session().controller().snapshot().orElse(null)).isPresent();
        setVisible(bindingPromptField, bindingLlm);
        boolean cacheEditor = ui.tab() == CopperGolemMenuUiState.Tab.BINDINGS && !gathering && bindingDetailVisible
                && filterTextEntryVisible
                && selectedBinding(lifecycle.session().controller().snapshot().orElse(null)).isPresent();
        setVisible(cacheValueField, cacheEditor);
        if (cacheEditor) {
            var bounds = CopperGolemMenuPanelLayout.bounds(width, height);
            cacheValueField.setX(bounds.x() + 18);
            cacheValueField.setY(bounds.y() + 56);
        }
    }

    private java.util.Optional<dev.totem.automata.network.CopperWrenchBindingsPayload.BindingEntry> selectedBinding(
            dev.totem.automata.network.CopperWrenchBindingsPayload snapshot) {
        if (snapshot == null || ui.selected() < 0 || ui.selected() >= snapshot.bindings().size()) return java.util.Optional.empty();
        return java.util.Optional.of(snapshot.bindings().get(ui.selected()));
    }

    private int bindingIndexAt(double mouseX, double mouseY, int bindingCount) {
        var bounds = CopperGolemMenuPanelLayout.bounds(width, height);
        int visible = Math.min(MAX_VISIBLE_SORTING_TARGETS, Math.max(0, bindingCount - ui.scroll()));
        if (mouseX < bounds.x() + 146 || mouseX >= bounds.x() + 164) return -1;
        for (int cell = 0; cell < visible; cell++) {
            int targetY = sortingTargetY(bounds.y(), visible, cell);
            if (mouseY >= targetY && mouseY < targetY + 18) return ui.scroll() + cell;
        }
        return -1;
    }

    private ItemStack playerInventoryItemAt(double mouseX, double mouseY) {
        for (int index = CopperGolemMenuLayout.GOLEM_SLOT_COUNT; index < menu.slots.size(); index++) {
            Slot slot = menu.slots.get(index);
            if (slot.isActive() && slot.hasItem()
                    && isInside(mouseX, mouseY, leftPos + slot.x, topPos + slot.y, 16, 16)) {
                return slot.getItem();
            }
        }
        return ItemStack.EMPTY;
    }

    private FilterEntry filterEntryAt(double mouseX, double mouseY, CopperGolemMenuPanelLayout.Bounds bounds,
                                      dev.totem.automata.network.CopperWrenchBindingsPayload snapshot) {
        var binding = selectedBinding(snapshot).orElse(null);
        if (binding == null) return null;
        for (boolean allowed : List.of(true, false)) {
            int paneX = bounds.x() + (allowed ? 8 : 94);
            int paneY = bounds.y() + 54;
            List<FilterEntry> entries = filterEntries(binding, allowed);
            for (int index = 0; index < Math.min(MAX_VISIBLE_FILTER_ENTRIES, entries.size()); index++) {
                int slotX = paneX + (index % FILTER_GRID_COLUMNS) * 18;
                int slotY = paneY + 20 + (index / FILTER_GRID_COLUMNS) * 18;
                if (isInside(mouseX, mouseY, slotX, slotY, 18, 18)) return entries.get(index);
            }
        }
        return null;
    }

    private Boolean filterDropTargetAt(double mouseX, double mouseY) {
        var snapshot = lifecycle.session().controller().snapshot().orElse(null);
        if (snapshot == null || ui.tab() != CopperGolemMenuUiState.Tab.BINDINGS || !bindingDetailVisible
                || filterTextEntryVisible || !"sorting".equals(snapshot.mode())) return null;
        var bounds = CopperGolemMenuPanelLayout.bounds(width, height);
        if (isInside(mouseX, mouseY, bounds.x() + 8, bounds.y() + 54, 76, 56)) return true;
        if (isInside(mouseX, mouseY, bounds.x() + 94, bounds.y() + 54, 76, 56)) return false;
        return null;
    }

    private ItemStack iconStack(String itemId) {
        Identifier identifier = Identifier.tryParse(itemId);
        Item item = identifier == null ? Items.BARRIER : BuiltInRegistries.ITEM.getOptional(identifier).orElse(Items.BARRIER);
        return new ItemStack(item == Items.AIR ? Items.BARRIER : item);
    }

    private Component blockDisplayName(String blockId) {
        Identifier identifier = Identifier.tryParse(blockId);
        return identifier == null ? Component.literal(blockId)
                : BuiltInRegistries.BLOCK.getOptional(identifier).map(block -> block.getName())
                .orElse(Component.literal(blockId));
    }

    private String trimToWidth(String value, int width) {
        return font.plainSubstrByWidth(value, Math.max(0, width));
    }

    private List<GatheringTarget> gatheringAcceptedTargets(dev.totem.automata.network.CopperWrenchBindingsPayload snapshot) {
        List<GatheringTarget> targets = new ArrayList<>();
        Set<String> manual = new LinkedHashSet<>();
        for (String value : snapshot.gatheringManualTargets()) {
            if (value != null && !value.isBlank() && manual.add(value)) {
                targets.add(new GatheringTarget(value, false, CopperGolemGatheringTargetPayload.TargetSet.MANUAL));
            }
        }
        addGatheringTargets(targets, snapshot.gatheringLlmAllowedBlockIds(), false, CopperGolemGatheringTargetPayload.TargetSet.ALLOWED, manual);
        addGatheringTargets(targets, snapshot.gatheringLlmAllowedTags(), true, CopperGolemGatheringTargetPayload.TargetSet.ALLOWED, Set.of());
        return targets;
    }

    private List<GatheringTarget> gatheringDeniedTargets(dev.totem.automata.network.CopperWrenchBindingsPayload snapshot) {
        List<GatheringTarget> targets = new ArrayList<>();
        addGatheringTargets(targets, snapshot.gatheringLlmDeniedBlockIds(), false, CopperGolemGatheringTargetPayload.TargetSet.DENIED, Set.of());
        addGatheringTargets(targets, snapshot.gatheringLlmDeniedTags(), true, CopperGolemGatheringTargetPayload.TargetSet.DENIED, Set.of());
        return targets;
    }

    private static void addGatheringTargets(List<GatheringTarget> targets, List<String> values, boolean tag,
                                            CopperGolemGatheringTargetPayload.TargetSet targetSet, Set<String> excluded) {
        for (String value : values) {
            if (value != null && !value.isBlank() && !excluded.contains(value)) {
                targets.add(new GatheringTarget(value, tag, targetSet));
            }
        }
    }

    private record GatheringTarget(String value, boolean tag, CopperGolemGatheringTargetPayload.TargetSet targetSet) { }

    private record FilterEntry(String value, boolean tag, boolean allowed) { }

    /** Slot positions and accessor are only live once the cutover-only client mixin is enabled. */
    private void updateMenuSlotLayout(CopperGolemMenuPanelLayout.Bounds bounds) {
        if (menu.slots.size() < CopperGolemMenuLayout.GOLEM_SLOT_COUNT) return;
        int inventoryX = 8;
        int inventoryY = 139;
        int hotbarY = 197;
        setSlotPosition(menu.slots.get(CopperGolemMenuLayout.SLOT_FUEL), 119, 71);
        setSlotPosition(menu.slots.get(CopperGolemMenuLayout.SLOT_GATHERING_TOOL), 96, 42);
        setSlotPosition(menu.slots.get(CopperGolemMenuLayout.SLOT_GATHERING_STORAGE), 119, 42);
        int index = CopperGolemMenuLayout.GOLEM_SLOT_COUNT;
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9 && index < menu.slots.size(); column++) {
                setSlotPosition(menu.slots.get(index++), inventoryX + column * 18, inventoryY + row * 18);
            }
        }
        for (int column = 0; column < 9 && index < menu.slots.size(); column++) {
            setSlotPosition(menu.slots.get(index++), inventoryX + column * 18, hotbarY);
        }
    }

    private void positionInventoryLabel(CopperGolemMenuPanelLayout.Bounds bounds) {
        inventoryLabelX = 8;
        inventoryLabelY = 127;
    }

    private static void setSlotPosition(Slot slot, int x, int y) {
        SlotAccessor accessor = (SlotAccessor) slot;
        accessor.totemAutomata$setX(x);
        accessor.totemAutomata$setY(y);
    }

    private void drawSlotBackings(GuiGraphicsExtractor graphics) {
        for (int index = 0; index < menu.slots.size(); index++) {
            Slot slot = menu.slots.get(index);
            if (!slot.isActive()) continue;
            int x = leftPos + slot.x - 1;
            int y = topPos + slot.y - 1;
            renderSlot(graphics, index == CopperGolemMenuLayout.SLOT_GATHERING_TOOL ? TOOL_SLOT : SLOT, x, y);
        }
    }

    private static void setVisible(net.minecraft.client.gui.components.AbstractWidget widget, boolean visible) {
        widget.visible = visible;
        widget.active = visible;
    }
}
