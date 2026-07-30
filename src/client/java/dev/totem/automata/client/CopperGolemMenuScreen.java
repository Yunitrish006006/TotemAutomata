package dev.totem.automata.client;

import dev.totem.automata.menu.CopperGolemMenu;
import dev.totem.automata.menu.CopperGolemMenuLayout;
import dev.totem.automata.mixin.client.SlotAccessor;
import dev.totem.automata.network.CopperGolemGatheringTargetPayload;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
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
public final class CopperGolemMenuScreen extends AbstractContainerScreen<CopperGolemMenu> {
    private static final int INFO_ICON_SIZE = 20;
    private static final int INFO_ICON_STRIDE = 24;
    private static final int BINDING_ICON_Y = 82;
    private static final int GATHERING_ACCEPTED_ICON_Y = 84;
    private static final int GATHERING_DENIED_ICON_Y = 110;

    private final CopperGolemMenuScreenLifecycle lifecycle;
    private final CopperGolemMenuUiState ui = new CopperGolemMenuUiState();
    private Button operationButton;
    private Button modeButton;
    private Button bindingsTabButton;
    private Button llmTabButton;
    private Button saveApiButton;
    private Button testApiButton;
    private Button gatheringLlmToggleButton;
    private Button saveGatheringPromptButton;
    private Button bindingLlmToggleButton;
    private Button saveBindingPromptButton;
    private Button cacheTypeButton;
    private Button cacheDestinationButton;
    private Button moveCacheButton;
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
    private boolean cacheValueAllowed = true;

    public CopperGolemMenuScreen(CopperGolemMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, CopperGolemMenuPanelLayout.PREFERRED_WIDTH, CopperGolemMenuPanelLayout.PREFERRED_HEIGHT);
        lifecycle = new CopperGolemMenuScreenLifecycle(menu, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        lifecycle.open();
        var bounds = CopperGolemMenuPanelLayout.bounds(width, height);
        leftPos = bounds.x();
        topPos = bounds.y();
        updateMenuSlotLayout(bounds);
        positionInventoryLabel(bounds);
        operationButton = addRenderableWidget(Button.builder(operationText(), button -> lifecycle.session().toggleOperation())
                .bounds(bounds.x() + bounds.width() - 86, bounds.y() + 7, 74, 18).build());
        modeButton = addRenderableWidget(Button.builder(modeText(), button -> lifecycle.session().switchMode())
                .bounds(bounds.x() + bounds.width() - 170, bounds.y() + 7, 78, 18).build());
        bindingsTabButton = addRenderableWidget(Button.builder(tabText(CopperGolemMenuUiState.Tab.BINDINGS), button -> selectTab(CopperGolemMenuUiState.Tab.BINDINGS))
                .bounds(bounds.x() + 12, bounds.y() + 26, 70, 18).build());
        llmTabButton = addRenderableWidget(Button.builder(tabText(CopperGolemMenuUiState.Tab.LLM), button -> selectTab(CopperGolemMenuUiState.Tab.LLM))
                .bounds(bounds.x() + 88, bounds.y() + 26, 70, 18).build());
        int editorX = bounds.x() + 12;
        int editorY = bounds.y() + 86;
        int editorWidth = settingsWidth(bounds);
        apiUrlField = addRenderableWidget(editor(Component.translatable("message.deadrecall.copper_wrench.llm_api_url"), editorX, editorY, editorWidth, 2048,
                Component.literal("https://api.openai.com/v1/chat/completions")));
        apiKeyField = addRenderableWidget(editor(Component.translatable("message.deadrecall.copper_wrench.llm_api_key"), editorX, editorY + 24, editorWidth, 512,
                Component.literal("sk-…")));
        modelField = addRenderableWidget(editor(Component.translatable("message.deadrecall.copper_wrench.llm_model"), editorX, editorY + 48, editorWidth, 256,
                Component.literal("gpt-4o-mini")));
        saveApiButton = addRenderableWidget(Button.builder(Component.translatable("message.deadrecall.copper_wrench.save_api"), button ->
                lifecycle.session().saveApiConfig(apiUrlField.getValue(), apiKeyField.getValue(), modelField.getValue()))
                .bounds(editorX, editorY + 74, 116, 18).build());
        testApiButton = addRenderableWidget(Button.builder(Component.translatable("message.deadrecall.copper_wrench.test_connection"), button ->
                lifecycle.session().testApiConnection(apiUrlField.getValue(), apiKeyField.getValue(), modelField.getValue()))
                .bounds(editorX + 122, editorY + 74, 122, 18).build());
        gatheringLlmToggleButton = addRenderableWidget(Button.builder(Component.translatable("message.deadrecall.copper_wrench.gathering_llm_prompt"), button -> toggleGatheringLlm())
                .bounds(editorX, editorY + 108, 124, 18).build());
        gatheringPromptField = addRenderableWidget(editor(Component.translatable("message.deadrecall.copper_wrench.gathering_llm_prompt"), editorX, editorY + 132, editorWidth, 2048,
                Component.translatable("message.deadrecall.copper_wrench.prompt_hint")));
        saveGatheringPromptButton = addRenderableWidget(Button.builder(Component.translatable("message.deadrecall.copper_wrench.save"), button -> saveGatheringPrompt())
                .bounds(editorX, editorY + 156, 74, 18).build());
        int bindingControlsY = bindingControlsY(bounds);
        bindingLlmToggleButton = addRenderableWidget(Button.builder(Component.translatable("message.deadrecall.copper_wrench.binding_llm_prompt"), button -> toggleBindingLlm())
                .bounds(editorX, bindingControlsY, 110, 18).build());
        int promptWidth = Math.max(80, editorWidth - 58);
        bindingPromptField = addRenderableWidget(editor(Component.translatable("message.deadrecall.copper_wrench.binding_llm_prompt"), editorX, bindingControlsY + 20, promptWidth, 2048,
                Component.translatable("message.deadrecall.copper_wrench.binding_prompt_hint")));
        saveBindingPromptButton = addRenderableWidget(Button.builder(Component.translatable("message.deadrecall.copper_wrench.save"), button -> saveBindingPrompt())
                .bounds(editorX + promptWidth + 4, bindingControlsY + 20, 54, 18).build());
        int cacheValueWidth = Math.max(64, editorWidth - 142);
        cacheValueField = addRenderableWidget(editor(Component.translatable("message.deadrecall.copper_wrench.cache_value"), editorX, bindingControlsY + 40, cacheValueWidth, 256,
                Component.literal("minecraft:iron_ingot")));
        cacheTypeButton = addRenderableWidget(Button.builder(Component.literal("Item"), button -> {
                    cacheValueIsTag = !cacheValueIsTag;
                    updateCacheButtons();
                }).bounds(editorX + cacheValueWidth, bindingControlsY + 40, 42, 18).build());
        cacheDestinationButton = addRenderableWidget(Button.builder(Component.literal("Allow"), button -> {
                    cacheValueAllowed = !cacheValueAllowed;
                    updateCacheButtons();
                }).bounds(editorX + cacheValueWidth + 42, bindingControlsY + 40, 54, 18).build());
        moveCacheButton = addRenderableWidget(Button.builder(Component.literal("Move"), button -> moveCachedDecision())
                .bounds(editorX + cacheValueWidth + 96, bindingControlsY + 40, 46, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(bounds.x() + bounds.width() / 2 - 45, bounds.y() + bounds.height() - 21, 90, 18).build());
        updateEditorVisibility();
    }

    private void refreshSnapshotState() {
        lifecycle.session().controller().snapshot().ifPresent(snapshot -> {
            menu.setGatheringSlotsVisible("gathering".equals(snapshot.mode()));
            if (!snapshot.golemId().equals(editorGolem) || snapshot.revision() != editorRevision) {
                apiUrlField.setValue(snapshot.llmApiUrl());
                apiKeyField.setValue(snapshot.llmApiKey());
                modelField.setValue(snapshot.llmModel());
                gatheringPromptField.setValue(snapshot.gatheringLlmPrompt());
                editorGolem = snapshot.golemId();
                editorRevision = snapshot.revision();
            }
            operationButton.setMessage(operationText());
            modeButton.setMessage(modeText());
            gatheringLlmToggleButton.setMessage(Component.translatable("message.deadrecall.copper_wrench.llm_state",
                    Component.translatable(snapshot.gatheringLlmEnabled()
                            ? "message.deadrecall.copper_wrench.enabled"
                            : "message.deadrecall.copper_wrench.disabled")));
            refreshBindingEditor(snapshot);
        });
        bindingsTabButton.setMessage(tabText(CopperGolemMenuUiState.Tab.BINDINGS));
        llmTabButton.setMessage(tabText(CopperGolemMenuUiState.Tab.LLM));
        updateEditorVisibility();
    }

    @Override
    public void removed() {
        lifecycle.close();
        super.removed();
    }

    /** Supplies deterministic, non-secret state to the client visual GameTest. */
    void acceptSnapshotForVisualTest(dev.totem.automata.network.CopperWrenchBindingsPayload snapshot) {
        lifecycle.session().accept(snapshot);
    }

    /** Selects a binding without sending input, solely for the deterministic visual GameTest. */
    void selectBindingForVisualTest(int index) {
        lifecycle.session().controller().snapshot().ifPresent(snapshot -> ui.select(index, snapshot.bindings().size()));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        var snapshot = lifecycle.session().controller().snapshot().orElse(null);
        if (ui.tab() == CopperGolemMenuUiState.Tab.BINDINGS && snapshot != null && "sorting".equals(snapshot.mode())) {
            int index = bindingIndexAt(event.x(), event.y(), snapshot.bindings().size());
            if (index >= 0) {
                ui.select(index, snapshot.bindings().size());
                bindingEditorIndex = -1;
                return true;
            }
        }
        if (ui.tab() == CopperGolemMenuUiState.Tab.BINDINGS && snapshot != null && "gathering".equals(snapshot.mode())
                && event.button() == 1) {
            gatheringTargetHitAt(snapshot, event.x(), event.y()).ifPresent(hit -> lifecycle.session().updateGatheringTarget(
                    hit.value(), hit.tag(), hit.targetSet(), CopperGolemGatheringTargetPayload.Action.REMOVE));
            if (gatheringTargetHitAt(snapshot, event.x(), event.y()).isPresent()) return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        var snapshot = lifecycle.session().controller().snapshot().orElse(null);
        if (ui.tab() != CopperGolemMenuUiState.Tab.BINDINGS || snapshot == null || verticalAmount == 0) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
        var bounds = CopperGolemMenuPanelLayout.bounds(width, height);
        if ("sorting".equals(snapshot.mode())) {
            int max = Math.max(0, snapshot.bindings().size() - visibleBindingIcons(bounds));
            ui.scroll(ui.scroll() + (verticalAmount < 0 ? 1 : -1), max);
            return true;
        }
        if ("gathering".equals(snapshot.mode())) {
            int visible = visibleGatheringTargetIcons(bounds);
            int targetCount = Math.max(gatheringAcceptedTargets(snapshot).size(), gatheringDeniedTargets(snapshot).size());
            gatheringTargetScroll = Math.max(0, Math.min(gatheringTargetScroll + (verticalAmount < 0 ? 1 : -1),
                    Math.max(0, targetCount - visible)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tick) {
        graphics.fill(0, 0, width, height, 0xC0000000);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tick) {
        refreshSnapshotState();
        var bounds = CopperGolemMenuPanelLayout.bounds(width, height);
        graphics.fill(bounds.x(), bounds.y(), bounds.x() + bounds.width(), bounds.y() + bounds.height(), 0xFF181818);
        graphics.outline(bounds.x(), bounds.y(), bounds.width(), bounds.height(), 0xFF6A6A6A);
        graphics.text(font, title, bounds.x() + CopperGolemMenuPanelLayout.PADDING, bounds.y() + 9, 0xFFFFFFFF);
        lifecycle.session().controller().snapshot().ifPresentOrElse(
                snapshot -> drawSnapshot(graphics, bounds, snapshot, mouseX, mouseY),
                () -> graphics.text(font, Component.translatable("message.deadrecall.copper_wrench.ui_waiting_state"),
                        bounds.x() + 12, bounds.y() + 58, 0xFFFFC857));
        drawSlotBackings(graphics);
        super.extractRenderState(graphics, mouseX, mouseY, tick);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFFB8B8B8, false);
    }

    private void drawSnapshot(GuiGraphicsExtractor graphics, CopperGolemMenuPanelLayout.Bounds bounds,
                              dev.totem.automata.network.CopperWrenchBindingsPayload snapshot,
                              int mouseX, int mouseY) {
        int x = bounds.x() + 12;
        int y = bounds.y() + 54;
        int activityColor = snapshot.running() && snapshot.activity().startsWith("blocked_") ? 0xFFFFC857
                : snapshot.running() ? 0xFF64D26D : 0xFFFF6B6B;
        drawInfoIcon(graphics, operationStatusIcon(snapshot), x, y - 4, activityColor, mouseX, mouseY,
                List.of(
                        Component.translatable("message.deadrecall.copper_wrench.current_activity"),
                        Component.translatable("message.deadrecall.copper_wrench.activity_" + snapshot.activity())
                ));
        ItemStack fuelIcon = snapshot.fuelCount() > 0
                ? iconStack(snapshot.fuelItemId())
                : new ItemStack(Items.COAL);
        drawInfoIcon(graphics, fuelIcon, x + INFO_ICON_STRIDE, y - 4,
                snapshot.fuelCount() > 0 || snapshot.fuelTicks() > 0 ? 0xFFFFB238 : 0xFF777777,
                mouseX, mouseY, List.of(Component.translatable("message.deadrecall.copper_wrench.ui_fuel",
                        snapshot.fuelItemId(), snapshot.fuelCount(), snapshot.fuelTicks())));
        if (ui.tab() == CopperGolemMenuUiState.Tab.LLM) {
            Component apiState = snapshot.llmApiUrl().isBlank()
                    ? Component.translatable("message.deadrecall.copper_wrench.llm_not_configured")
                    : Component.literal(snapshot.llmApiUrl());
            drawInfoIcon(graphics, new ItemStack(Items.WRITABLE_BOOK), x + INFO_ICON_STRIDE * 2, y - 4,
                    snapshot.llmApiUrl().isBlank() ? 0xFF777777 : 0xFF64D26D, mouseX, mouseY,
                    List.of(
                            Component.translatable("message.deadrecall.copper_wrench.llm_state", apiState),
                            Component.translatable("message.deadrecall.copper_wrench.llm_active_count",
                                    snapshot.llmActiveCount())
                    ));
            if ("gathering".equals(snapshot.mode())) {
                drawInfoIcon(graphics, new ItemStack(Items.BOOK), x + INFO_ICON_STRIDE * 3, y - 4,
                        0xFF6A8FC7, mouseX, mouseY,
                        List.of(Component.translatable("message.deadrecall.copper_wrench.gathering_cache_summary",
                                snapshot.gatheringLlmCachedBlockIds(), snapshot.gatheringLlmCachedTags())));
            }
            return;
        }
        if ("sorting".equals(snapshot.mode())) {
            drawBindingTab(graphics, bounds, snapshot, mouseX, mouseY);
            return;
        }
        drawGatheringTab(graphics, bounds, snapshot, mouseX, mouseY);
    }

    private void drawBindingTab(GuiGraphicsExtractor graphics, CopperGolemMenuPanelLayout.Bounds bounds,
                                dev.totem.automata.network.CopperWrenchBindingsPayload snapshot,
                                int mouseX, int mouseY) {
        int x = bounds.x() + 12;
        int y = bounds.y() + BINDING_ICON_Y;
        var source = snapshot.sourceContainer();
        drawInfoIcon(graphics,
                source == null ? new ItemStack(Items.CHEST) : iconStack(source.itemId()),
                x, y, source == null ? 0xFF777777 : 0xFFB97836,
                mouseX, mouseY, sourceTooltip(source));

        int visible = visibleBindingIcons(bounds);
        for (int cell = 0; cell < visible; cell++) {
            int index = ui.scroll() + cell;
            if (index >= snapshot.bindings().size()) break;
            var binding = snapshot.bindings().get(index);
            boolean selected = index == ui.selected();
            int border = selected ? 0xFFE2C15A : binding.available() ? 0xFF4C8A53 : binding.loaded() ? 0xFF9A4D4D : 0xFF777777;
            drawInfoIcon(graphics, iconStack(binding.itemId()),
                    x + 28 + cell * INFO_ICON_STRIDE, y, border,
                    mouseX, mouseY, bindingTooltip(binding, index));
        }
    }

    private void drawGatheringTab(GuiGraphicsExtractor graphics, CopperGolemMenuPanelLayout.Bounds bounds,
                                  dev.totem.automata.network.CopperWrenchBindingsPayload snapshot,
                                  int mouseX, int mouseY) {
        int x = bounds.x() + 12;
        int y = bounds.y() + 50;
        List<GatheringTarget> accepted = gatheringAcceptedTargets(snapshot);
        List<GatheringTarget> denied = gatheringDeniedTargets(snapshot);
        drawInfoIcon(graphics,
                snapshot.gatheringToolCount() > 0 ? iconStack(snapshot.gatheringToolItemId()) : new ItemStack(Items.IRON_PICKAXE),
                x + INFO_ICON_STRIDE * 2, y, snapshot.gatheringToolCount() > 0 ? 0xFF6A8FC7 : 0xFF777777,
                mouseX, mouseY, List.of(Component.translatable("message.deadrecall.copper_wrench.gathering_tool_summary",
                        snapshot.gatheringToolItemId(), snapshot.gatheringToolCount())));
        drawInfoIcon(graphics,
                snapshot.gatheringStorageCount() > 0 ? iconStack(snapshot.gatheringStorageItemId()) : new ItemStack(Items.CHEST),
                x + INFO_ICON_STRIDE * 3, y, snapshot.gatheringStorageCount() > 0 ? 0xFFB97836 : 0xFF777777,
                mouseX, mouseY, List.of(Component.translatable("message.deadrecall.copper_wrench.gathering_storage_summary",
                        snapshot.gatheringStorageItemId(), snapshot.gatheringStorageCount())));
        var source = snapshot.sourceContainer();
        drawInfoIcon(graphics,
                source == null ? new ItemStack(Items.CHEST) : iconStack(source.itemId()),
                x + INFO_ICON_STRIDE * 4, y, source == null ? 0xFF777777 : 0xFFB97836,
                mouseX, mouseY, sourceTooltip(source));

        drawGatheringTargetIcons(graphics, accepted, x, bounds.y() + GATHERING_ACCEPTED_ICON_Y,
                0xFF4C8A53, Component.translatable("message.deadrecall.copper_wrench.accepted_targets"),
                mouseX, mouseY, bounds);
        drawGatheringTargetIcons(graphics, denied, x, bounds.y() + GATHERING_DENIED_ICON_Y,
                0xFF9A4D4D, Component.translatable("message.deadrecall.copper_wrench.denied_targets"),
                mouseX, mouseY, bounds);
    }

    private void drawGatheringTargetIcons(GuiGraphicsExtractor graphics, List<GatheringTarget> targets,
                                          int x, int y, int color, Component group,
                                          int mouseX, int mouseY, CopperGolemMenuPanelLayout.Bounds bounds) {
        int visible = visibleGatheringTargetIcons(bounds);
        for (int cell = 0; cell < visible; cell++) {
            int index = gatheringTargetScroll + cell;
            if (index >= targets.size()) break;
            GatheringTarget target = targets.get(index);
            drawInfoIcon(graphics,
                    target.tag() ? new ItemStack(Items.NAME_TAG) : iconStack(target.value()),
                    x + cell * INFO_ICON_STRIDE, y, color, mouseX, mouseY,
                    gatheringTargetTooltip(target, group));
        }
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

    private List<Component> sourceTooltip(
            dev.totem.automata.network.CopperWrenchBindingsPayload.BindingEntry source) {
        if (source == null) {
            return List.of(Component.translatable("message.deadrecall.copper_wrench.ui_source",
                    Component.translatable("message.deadrecall.copper_wrench.source_unbound")));
        }
        return List.of(
                Component.translatable("message.deadrecall.copper_wrench.ui_source",
                        blockDisplayName(source.blockId())),
                Component.literal(source.blockId()),
                Component.translatable("message.deadrecall.copper_wrench.ui_container_location",
                        source.dimension(), source.x(), source.y(), source.z()),
                bindingStatusTooltip(source)
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
                        binding.llmCachedItemIds(), binding.llmCachedTags())
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

    private Component tabText(CopperGolemMenuUiState.Tab tab) {
        String key = tab == CopperGolemMenuUiState.Tab.BINDINGS ? "tab_bindings" : "tab_llm";
        Component label = Component.translatable("message.deadrecall.copper_wrench." + key);
        return tab == ui.tab() ? Component.literal("[").append(label).append(Component.literal("]")) : label;
    }

    private EditBox editor(Component label, int x, int y, int width, int maxLength, Component hint) {
        EditBox field = new EditBox(font, x, y, width, 18, label);
        field.setMaxLength(maxLength);
        field.setHint(hint);
        return field;
    }

    private void toggleGatheringLlm() {
        lifecycle.session().controller().snapshot().ifPresent(snapshot ->
                lifecycle.session().updateGatheringLlm(!snapshot.gatheringLlmEnabled(), gatheringPromptField.getValue()));
    }

    private void saveGatheringPrompt() {
        lifecycle.session().controller().snapshot().ifPresent(snapshot ->
                lifecycle.session().updateGatheringLlm(snapshot.gatheringLlmEnabled(), gatheringPromptField.getValue()));
    }

    private void selectTab(CopperGolemMenuUiState.Tab tab) {
        ui.tab(tab);
        updateEditorVisibility();
    }

    private void toggleBindingLlm() {
        selectedBinding(lifecycle.session().controller().snapshot().orElse(null)).ifPresent(binding ->
                lifecycle.session().updateBindingLlm(ui.selected(), !binding.llmEnabled(), bindingPromptField.getValue()));
    }

    private void saveBindingPrompt() {
        selectedBinding(lifecycle.session().controller().snapshot().orElse(null)).ifPresent(binding ->
                lifecycle.session().updateBindingLlm(ui.selected(), binding.llmEnabled(), bindingPromptField.getValue()));
    }

    private void moveCachedDecision() {
        if (cacheValueField.getValue().isBlank()) return;
        selectedBinding(lifecycle.session().controller().snapshot().orElse(null)).ifPresent(binding ->
                lifecycle.session().moveCachedDecision(ui.selected(), cacheValueField.getValue(), cacheValueIsTag, cacheValueAllowed));
    }

    private void refreshBindingEditor(dev.totem.automata.network.CopperWrenchBindingsPayload snapshot) {
        selectedBinding(snapshot).ifPresentOrElse(binding -> {
            if (ui.selected() != bindingEditorIndex || snapshot.revision() != bindingEditorRevision) {
                bindingPromptField.setValue(binding.llmPrompt());
                bindingEditorIndex = ui.selected();
                bindingEditorRevision = snapshot.revision();
            }
            bindingLlmToggleButton.setMessage(Component.translatable("message.deadrecall.copper_wrench.llm_state",
                    Component.translatable(binding.llmEnabled()
                            ? "message.deadrecall.copper_wrench.enabled"
                            : "message.deadrecall.copper_wrench.disabled")));
        }, () -> bindingEditorIndex = -1);
        updateCacheButtons();
    }

    private void updateCacheButtons() {
        cacheTypeButton.setMessage(Component.translatable(cacheValueIsTag
                ? "message.deadrecall.copper_wrench.entry_type_tag"
                : "message.deadrecall.copper_wrench.entry_type_item"));
        cacheDestinationButton.setMessage(Component.translatable(cacheValueAllowed
                ? "message.deadrecall.copper_wrench.cache_side_accepted"
                : "message.deadrecall.copper_wrench.cache_side_denied"));
        moveCacheButton.setMessage(Component.translatable("message.deadrecall.copper_wrench.cache_move_button"));
    }

    private void updateEditorVisibility() {
        boolean llmTab = ui.tab() == CopperGolemMenuUiState.Tab.LLM;
        boolean gathering = lifecycle.session().controller().snapshot()
                .map(snapshot -> "gathering".equals(snapshot.mode())).orElse(false);
        setVisible(apiUrlField, llmTab);
        setVisible(apiKeyField, llmTab);
        setVisible(modelField, llmTab);
        setVisible(saveApiButton, llmTab);
        setVisible(testApiButton, llmTab);
        setVisible(gatheringLlmToggleButton, llmTab && gathering);
        setVisible(gatheringPromptField, llmTab && gathering);
        setVisible(saveGatheringPromptButton, llmTab && gathering);
        boolean bindingEditor = ui.tab() == CopperGolemMenuUiState.Tab.BINDINGS && !gathering
                && selectedBinding(lifecycle.session().controller().snapshot().orElse(null)).isPresent();
        setVisible(bindingLlmToggleButton, bindingEditor);
        setVisible(bindingPromptField, bindingEditor);
        setVisible(saveBindingPromptButton, bindingEditor);
        setVisible(cacheValueField, bindingEditor);
        setVisible(cacheTypeButton, bindingEditor);
        setVisible(cacheDestinationButton, bindingEditor);
        setVisible(moveCacheButton, bindingEditor);
    }

    private java.util.Optional<dev.totem.automata.network.CopperWrenchBindingsPayload.BindingEntry> selectedBinding(
            dev.totem.automata.network.CopperWrenchBindingsPayload snapshot) {
        if (snapshot == null || ui.selected() < 0 || ui.selected() >= snapshot.bindings().size()) return java.util.Optional.empty();
        return java.util.Optional.of(snapshot.bindings().get(ui.selected()));
    }

    private int bindingIndexAt(double mouseX, double mouseY, int bindingCount) {
        var bounds = CopperGolemMenuPanelLayout.bounds(width, height);
        int startX = bounds.x() + 40;
        int y = bounds.y() + BINDING_ICON_Y;
        if (mouseY < y || mouseY >= y + INFO_ICON_SIZE || mouseX < startX) return -1;
        int cell = (int) ((mouseX - startX) / INFO_ICON_STRIDE);
        if (cell < 0 || cell >= visibleBindingIcons(bounds)
                || mouseX >= startX + cell * INFO_ICON_STRIDE + INFO_ICON_SIZE) return -1;
        int index = ui.scroll() + cell;
        return index < bindingCount ? index : -1;
    }

    private static int settingsWidth(CopperGolemMenuPanelLayout.Bounds bounds) {
        return Math.max(120, bounds.width() - 188);
    }

    private static int bindingControlsY(CopperGolemMenuPanelLayout.Bounds bounds) {
        return CopperGolemMenuPanelLayout.bindingControlsY(bounds);
    }

    private static int bindingListY(CopperGolemMenuPanelLayout.Bounds bounds) {
        return CopperGolemMenuPanelLayout.bindingListY(bounds);
    }

    private static int visibleBindingIcons(CopperGolemMenuPanelLayout.Bounds bounds) {
        return Math.max(1, (settingsWidth(bounds) - 28) / INFO_ICON_STRIDE);
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

    private java.util.Optional<GatheringTarget> gatheringTargetHitAt(
            dev.totem.automata.network.CopperWrenchBindingsPayload snapshot, double mouseX, double mouseY) {
        var bounds = CopperGolemMenuPanelLayout.bounds(width, height);
        int startX = bounds.x() + 12;
        if (mouseX < startX) return java.util.Optional.empty();
        int cell = (int) ((mouseX - startX) / INFO_ICON_STRIDE);
        if (cell < 0 || cell >= visibleGatheringTargetIcons(bounds)
                || mouseX >= startX + cell * INFO_ICON_STRIDE + INFO_ICON_SIZE) {
            return java.util.Optional.empty();
        }
        List<GatheringTarget> targets;
        int acceptedY = bounds.y() + GATHERING_ACCEPTED_ICON_Y;
        int deniedY = bounds.y() + GATHERING_DENIED_ICON_Y;
        if (mouseY >= acceptedY && mouseY < acceptedY + INFO_ICON_SIZE) {
            targets = gatheringAcceptedTargets(snapshot);
        } else if (mouseY >= deniedY && mouseY < deniedY + INFO_ICON_SIZE) {
            targets = gatheringDeniedTargets(snapshot);
        } else {
            return java.util.Optional.empty();
        }
        int index = gatheringTargetScroll + cell;
        return index < targets.size() ? java.util.Optional.of(targets.get(index)) : java.util.Optional.empty();
    }

    private static int visibleGatheringTargetIcons(CopperGolemMenuPanelLayout.Bounds bounds) {
        return Math.max(1, settingsWidth(bounds) / INFO_ICON_STRIDE);
    }

    private record GatheringTarget(String value, boolean tag, CopperGolemGatheringTargetPayload.TargetSet targetSet) { }

    /** Slot positions and accessor are only live once the cutover-only client mixin is enabled. */
    private void updateMenuSlotLayout(CopperGolemMenuPanelLayout.Bounds bounds) {
        if (menu.slots.size() < CopperGolemMenuLayout.GOLEM_SLOT_COUNT) return;
        int inventoryX = Math.max(180, bounds.width() - 174);
        int hotbarY = Math.max(136, bounds.height() - 100);
        int inventoryY = hotbarY - 58;
        setSlotPosition(menu.slots.get(CopperGolemMenuLayout.SLOT_FUEL), Math.max(160, inventoryX - 56), 26);
        setSlotPosition(menu.slots.get(CopperGolemMenuLayout.SLOT_GATHERING_TOOL), Math.max(120, inventoryX - 106), Math.max(74, inventoryY - 72));
        setSlotPosition(menu.slots.get(CopperGolemMenuLayout.SLOT_GATHERING_STORAGE), Math.max(120, inventoryX - 60), Math.max(74, inventoryY - 72));
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
        int inventoryX = Math.max(180, bounds.width() - 174);
        int hotbarY = Math.max(136, bounds.height() - 100);
        int inventoryY = hotbarY - 58;
        inventoryLabelX = inventoryX;
        inventoryLabelY = inventoryY - 12;
    }

    private static void setSlotPosition(Slot slot, int x, int y) {
        SlotAccessor accessor = (SlotAccessor) slot;
        accessor.totemAutomata$setX(x);
        accessor.totemAutomata$setY(y);
    }

    private void drawSlotBackings(GuiGraphicsExtractor graphics) {
        for (Slot slot : menu.slots) {
            if (!slot.isActive()) continue;
            int x = leftPos + slot.x - 1;
            int y = topPos + slot.y - 1;
            graphics.fill(x, y, x + 18, y + 18, 0xC0101010);
            graphics.outline(x, y, 18, 18, 0xFF6A6A6A);
        }
    }

    private static void setVisible(net.minecraft.client.gui.components.AbstractWidget widget, boolean visible) {
        widget.visible = visible;
        widget.active = visible;
    }
}
