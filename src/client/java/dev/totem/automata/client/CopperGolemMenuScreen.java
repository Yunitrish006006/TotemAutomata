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
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

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
        apiUrlField = addRenderableWidget(editor(Component.literal("LLM API URL"), editorX, editorY, editorWidth, 2048,
                Component.literal("https://api.openai.com/v1/chat/completions")));
        apiKeyField = addRenderableWidget(editor(Component.literal("LLM API Key"), editorX, editorY + 24, editorWidth, 512,
                Component.literal("sk-…")));
        modelField = addRenderableWidget(editor(Component.literal("LLM Model"), editorX, editorY + 48, editorWidth, 256,
                Component.literal("gpt-4o-mini")));
        saveApiButton = addRenderableWidget(Button.builder(Component.translatable("message.deadrecall.copper_wrench.save_api"), button ->
                lifecycle.session().saveApiConfig(apiUrlField.getValue(), apiKeyField.getValue(), modelField.getValue()))
                .bounds(editorX, editorY + 74, 116, 18).build());
        testApiButton = addRenderableWidget(Button.builder(Component.translatable("message.deadrecall.copper_wrench.test_connection"), button ->
                lifecycle.session().testApiConnection(apiUrlField.getValue(), apiKeyField.getValue(), modelField.getValue()))
                .bounds(editorX + 122, editorY + 74, 122, 18).build());
        gatheringLlmToggleButton = addRenderableWidget(Button.builder(Component.literal("Gathering LLM"), button -> toggleGatheringLlm())
                .bounds(editorX, editorY + 108, 124, 18).build());
        gatheringPromptField = addRenderableWidget(editor(Component.literal("Gathering LLM Prompt"), editorX, editorY + 132, editorWidth, 2048,
                Component.translatable("message.deadrecall.copper_wrench.prompt_hint")));
        saveGatheringPromptButton = addRenderableWidget(Button.builder(Component.translatable("message.deadrecall.copper_wrench.save"), button -> saveGatheringPrompt())
                .bounds(editorX, editorY + 156, 74, 18).build());
        int bindingControlsY = bindingControlsY(bounds);
        bindingLlmToggleButton = addRenderableWidget(Button.builder(Component.literal("Binding LLM"), button -> toggleBindingLlm())
                .bounds(editorX, bindingControlsY, 110, 18).build());
        int promptWidth = Math.max(80, editorWidth - 58);
        bindingPromptField = addRenderableWidget(editor(Component.literal("Binding LLM Prompt"), editorX, bindingControlsY + 22, promptWidth, 2048,
                Component.translatable("message.deadrecall.copper_wrench.prompt_hint")));
        saveBindingPromptButton = addRenderableWidget(Button.builder(Component.translatable("message.deadrecall.copper_wrench.save"), button -> saveBindingPrompt())
                .bounds(editorX + promptWidth + 4, bindingControlsY + 22, 54, 18).build());
        int cacheValueWidth = Math.max(64, editorWidth - 142);
        cacheValueField = addRenderableWidget(editor(Component.literal("Cached item or tag"), editorX, bindingControlsY + 44, cacheValueWidth, 256,
                Component.literal("minecraft:iron_ingot")));
        cacheTypeButton = addRenderableWidget(Button.builder(Component.literal("Item"), button -> {
                    cacheValueIsTag = !cacheValueIsTag;
                    updateCacheButtons();
                }).bounds(editorX + cacheValueWidth, bindingControlsY + 44, 42, 18).build());
        cacheDestinationButton = addRenderableWidget(Button.builder(Component.literal("Allow"), button -> {
                    cacheValueAllowed = !cacheValueAllowed;
                    updateCacheButtons();
                }).bounds(editorX + cacheValueWidth + 42, bindingControlsY + 44, 54, 18).build());
        moveCacheButton = addRenderableWidget(Button.builder(Component.literal("Move"), button -> moveCachedDecision())
                .bounds(editorX + cacheValueWidth + 96, bindingControlsY + 44, 46, 18).build());
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
            gatheringLlmToggleButton.setMessage(Component.literal(snapshot.gatheringLlmEnabled()
                    ? "Gathering LLM: on" : "Gathering LLM: off"));
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
            int max = Math.max(0, snapshot.bindings().size() - visibleBindingRows(bounds));
            ui.scroll(ui.scroll() + (verticalAmount < 0 ? 1 : -1), max);
            return true;
        }
        if ("gathering".equals(snapshot.mode())) {
            int visible = visibleGatheringTargetRows(bounds);
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
                snapshot -> drawSnapshot(graphics, bounds, snapshot),
                () -> graphics.text(font, Component.literal("Waiting for Copper Golem state…"), bounds.x() + 12, bounds.y() + 58, 0xFFFFC857));
        drawSlotBackings(graphics);
        super.extractRenderState(graphics, mouseX, mouseY, tick);
    }

    private void drawSnapshot(GuiGraphicsExtractor graphics, CopperGolemMenuPanelLayout.Bounds bounds,
                              dev.totem.automata.network.CopperWrenchBindingsPayload snapshot) {
        int x = bounds.x() + 12;
        int y = bounds.y() + 54;
        int activityColor = snapshot.running() && snapshot.activity().startsWith("blocked_") ? 0xFFFFC857
                : snapshot.running() ? 0xFF64D26D : 0xFFFF6B6B;
        graphics.text(font, Component.literal("Status: " + snapshot.activity()), x, y, activityColor);
        graphics.text(font, Component.literal("Fuel: " + snapshot.fuelItemId() + " ×" + snapshot.fuelCount()
                + " (" + snapshot.fuelTicks() + " ticks)"), x, y + 14, 0xFFE0E0E0);
        if (ui.tab() == CopperGolemMenuUiState.Tab.LLM) {
            graphics.text(font, Component.literal("LLM: " + (snapshot.llmApiUrl().isBlank() ? "not configured" : snapshot.llmApiUrl())), x, y + 34, 0xFFE0E0E0);
            graphics.text(font, Component.literal("Active bindings: " + snapshot.llmActiveCount()), x, y + 48, 0xFFE0E0E0);
            if ("gathering".equals(snapshot.mode())) {
                graphics.text(font, Component.literal("Gathering cache: " + snapshot.gatheringLlmCachedBlockIds()
                        + " blocks, " + snapshot.gatheringLlmCachedTags() + " tags"), x, y + 62, 0xFFE0E0E0);
            }
            return;
        }
        if ("sorting".equals(snapshot.mode())) {
            drawBindingTab(graphics, bounds, snapshot);
            return;
        }
        drawGatheringTab(graphics, bounds, snapshot);
    }

    private void drawBindingTab(GuiGraphicsExtractor graphics, CopperGolemMenuPanelLayout.Bounds bounds,
                                dev.totem.automata.network.CopperWrenchBindingsPayload snapshot) {
        int x = bounds.x() + 12;
        int y = bounds.y() + 54;
        int listY = y + 42;
        int listWidth = settingsWidth(bounds);
        graphics.text(font, Component.literal("Source: " + bindingLocation(snapshot.sourceContainer())), x, y, 0xFFE0E0E0);
        graphics.text(font, Component.literal("Destinations: " + snapshot.bindings().size() + "  •  scroll to browse"), x, y + 14, 0xFFE0E0E0);
        int rows = visibleBindingRows(bounds);
        for (int row = 0; row < rows; row++) {
            int index = ui.scroll() + row;
            if (index >= snapshot.bindings().size()) break;
            var binding = snapshot.bindings().get(index);
            int cardY = listY + row * 28;
            boolean selected = index == ui.selected();
            int border = selected ? 0xFFE2C15A : binding.available() ? 0xFF4C8A53 : binding.loaded() ? 0xFF9A4D4D : 0xFF777777;
            graphics.fill(x, cardY, x + listWidth, cardY + 25, selected ? 0xC03A3322 : 0xB0222222);
            graphics.outline(x, cardY, listWidth, 25, border);
            String title = (index + 1) + ". " + binding.blockId() + " @ " + binding.x() + ", " + binding.y() + ", " + binding.z();
            graphics.text(font, Component.literal(trimToWidth(title, listWidth - 8)), x + 4, cardY + 4, 0xFFFFFFFF);
            String details = (binding.llmEnabled() ? "LLM on" : "LLM off") + "  •  cache "
                    + (binding.llmCachedItemIds() + binding.llmCachedTags());
            graphics.text(font, Component.literal(details), x + 4, cardY + 14, binding.available() ? 0xFFB8E8B8 : 0xFFE0C080);
        }
        if (snapshot.bindings().size() > rows) {
            graphics.text(font, Component.literal((ui.scroll() + 1) + "–"
                    + Math.min(snapshot.bindings().size(), ui.scroll() + rows) + " / " + snapshot.bindings().size()),
                    x + listWidth - 52, y + 14, 0xFFB8B8B8);
        }
        selectedBinding(snapshot).ifPresent(binding -> {
            int cacheY = bindingControlsY(bounds) - 18;
            String cache = "Allow " + (binding.llmAllowedItemIds().size() + binding.llmAllowedTags().size())
                    + "  •  Deny " + (binding.llmDeniedItemIds().size() + binding.llmDeniedTags().size());
            graphics.text(font, Component.literal(cache), x, cacheY, 0xFFB8B8B8);
        });
    }

    private void drawGatheringTab(GuiGraphicsExtractor graphics, CopperGolemMenuPanelLayout.Bounds bounds,
                                  dev.totem.automata.network.CopperWrenchBindingsPayload snapshot) {
        int x = bounds.x() + 12;
        int y = bounds.y() + 54;
        int width = settingsWidth(bounds);
        List<GatheringTarget> accepted = gatheringAcceptedTargets(snapshot);
        List<GatheringTarget> denied = gatheringDeniedTargets(snapshot);
        int groupGap = 8;
        int groupWidth = (width - groupGap) / 2;
        int groupY = bounds.y() + 132;
        graphics.text(font, Component.literal("Tool: " + snapshot.gatheringToolItemId() + " ×" + snapshot.gatheringToolCount()), x, y + 34, 0xFFE0E0E0);
        graphics.text(font, Component.literal("Storage: " + snapshot.gatheringStorageItemId() + " ×" + snapshot.gatheringStorageCount()), x, y + 48, 0xFFE0E0E0);
        graphics.text(font, Component.literal("Targets: " + (accepted.size() + denied.size()) + "  •  right-click to remove"), x, y + 62, 0xFFB8B8B8);
        drawGatheringTargetGroup(graphics, "Accepted", accepted, x, groupY, groupWidth, 0xFF4C8A53);
        drawGatheringTargetGroup(graphics, "Denied", denied, x + groupWidth + groupGap, groupY, groupWidth, 0xFF9A4D4D);
    }

    private void drawGatheringTargetGroup(GuiGraphicsExtractor graphics, String label, List<GatheringTarget> targets,
                                          int x, int y, int width, int color) {
        graphics.fill(x, y, x + width, y + 18, 0xB0222222);
        graphics.outline(x, y, width, 18, color);
        graphics.text(font, Component.literal(label + " (" + targets.size() + ")"), x + 4, y + 5, color);
        int visible = visibleGatheringTargetRows(CopperGolemMenuPanelLayout.bounds(this.width, this.height));
        for (int row = 0; row < visible; row++) {
            int index = gatheringTargetScroll + row;
            if (index >= targets.size()) break;
            GatheringTarget target = targets.get(index);
            int rowY = y + 20 + row * 18;
            graphics.fill(x, rowY, x + width, rowY + 16, 0x80101010);
            graphics.outline(x, rowY, width, 16, color);
            String prefix = target.targetSet() == CopperGolemGatheringTargetPayload.TargetSet.MANUAL ? "● " : target.tag() ? "# " : "• ";
            graphics.text(font, Component.literal(trimToWidth(prefix + target.value(), width - 8)), x + 4, rowY + 4, 0xFFE0E0E0);
        }
    }

    private Component operationText() {
        return lifecycle.session().controller().snapshot()
                .map(snapshot -> Component.translatable(snapshot.running()
                        ? "message.deadrecall.copper_wrench.action_stop"
                        : "message.deadrecall.copper_wrench.action_start"))
                .orElse(Component.translatable("message.deadrecall.copper_wrench.operation"));
    }

    private Component modeText() {
        return lifecycle.session().controller().snapshot()
                .map(snapshot -> Component.translatable("message.deadrecall.copper_wrench.mode_" + snapshot.mode()))
                .orElse(Component.translatable("message.deadrecall.copper_wrench.mode"));
    }

    private Component tabText(CopperGolemMenuUiState.Tab tab) {
        String key = tab == CopperGolemMenuUiState.Tab.BINDINGS ? "tab_bindings" : "tab_llm";
        Component label = Component.translatable("gui.deadrecall.copper_wrench." + key);
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
            bindingLlmToggleButton.setMessage(Component.literal(binding.llmEnabled() ? "Binding LLM: on" : "Binding LLM: off"));
        }, () -> bindingEditorIndex = -1);
        updateCacheButtons();
    }

    private void updateCacheButtons() {
        cacheTypeButton.setMessage(Component.literal(cacheValueIsTag ? "Tag" : "Item"));
        cacheDestinationButton.setMessage(Component.literal(cacheValueAllowed ? "Allow" : "Deny"));
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
        int row = (int) ((mouseY - (bounds.y() + 96)) / 28);
        if (mouseX < bounds.x() + 12 || mouseX >= bounds.x() + 12 + settingsWidth(bounds)
                || row < 0 || row >= visibleBindingRows(bounds)) return -1;
        int index = ui.scroll() + row;
        return index < bindingCount ? index : -1;
    }

    private static int settingsWidth(CopperGolemMenuPanelLayout.Bounds bounds) {
        return Math.max(120, bounds.width() - 188);
    }

    private static int bindingControlsY(CopperGolemMenuPanelLayout.Bounds bounds) {
        return bounds.y() + bounds.height() - 96;
    }

    private static int visibleBindingRows(CopperGolemMenuPanelLayout.Bounds bounds) {
        return Math.max(1, Math.min(3, (bindingControlsY(bounds) - (bounds.y() + 96) - 4) / 28));
    }

    private String bindingLocation(dev.totem.automata.network.CopperWrenchBindingsPayload.BindingEntry binding) {
        return binding == null ? "not configured" : binding.dimension() + " @ " + binding.x() + ", " + binding.y() + ", " + binding.z();
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
        int width = settingsWidth(bounds);
        int groupWidth = (width - 8) / 2;
        int groupY = bounds.y() + 132;
        int row = (int) ((mouseY - (groupY + 20)) / 18);
        if (row < 0 || row >= visibleGatheringTargetRows(bounds)) return java.util.Optional.empty();
        List<GatheringTarget> targets;
        if (mouseX >= bounds.x() + 12 && mouseX < bounds.x() + 12 + groupWidth) {
            targets = gatheringAcceptedTargets(snapshot);
        } else if (mouseX >= bounds.x() + 20 + groupWidth && mouseX < bounds.x() + 20 + groupWidth * 2) {
            targets = gatheringDeniedTargets(snapshot);
        } else return java.util.Optional.empty();
        int index = gatheringTargetScroll + row;
        return index < targets.size() ? java.util.Optional.of(targets.get(index)) : java.util.Optional.empty();
    }

    private static int visibleGatheringTargetRows(CopperGolemMenuPanelLayout.Bounds bounds) {
        return Math.max(1, (bounds.height() - 132 - 18 - 30) / 18);
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
