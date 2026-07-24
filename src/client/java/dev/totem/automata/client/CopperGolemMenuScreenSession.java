package dev.totem.automata.client;

import dev.totem.automata.network.CopperWrenchBindingsPayload;
import dev.totem.automata.network.CopperGolemGatheringTargetPayload;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Lifecycle and command seam for the future Automata Copper Golem screen.
 *
 * <p>The rendering class will create one session when it opens, render the
 * controller snapshot, and call these methods from its widgets.  No screen is
 * registered here, preserving DeadRecall's current UI authority.</p>
 */
public final class CopperGolemMenuScreenSession {
    private final UUID golemId;
    private final CopperGolemMenuClientController controller = new CopperGolemMenuClientController();
    private final CommandSender sender;
    private final Consumer<CopperWrenchBindingsPayload> payloadConsumer = controller::apply;

    public CopperGolemMenuScreenSession(UUID golemId) { this(golemId, CommandSender.network()); }
    CopperGolemMenuScreenSession(UUID golemId, CommandSender sender) {
        this.golemId = Objects.requireNonNull(golemId, "golemId");
        this.sender = Objects.requireNonNull(sender, "sender");
    }

    public void open() { CopperGolemMenuClientCutover.open(golemId, payloadConsumer); }
    public void close() { CopperGolemMenuClientCutover.close(payloadConsumer); }
    public CopperGolemMenuClientController controller() { return controller; }
    public void accept(CopperWrenchBindingsPayload payload) { if (golemId.equals(payload.golemId())) controller.apply(payload); }

    public void toggleOperation() { controller.toggleOperation().ifPresent(sender::operation); }
    public void switchMode() { controller.switchMode().ifPresent(sender::mode); }
    public void updateBindingLlm(int index, boolean enabled, String prompt) { controller.updateBindingLlm(index, enabled, prompt).ifPresent(sender::bindingLlm); }
    public void moveCachedDecision(int index, String value, boolean tag, boolean allowed) { controller.moveCachedDecision(index, value, tag, allowed).ifPresent(sender::bindingCache); }
    public void updateGatheringLlm(boolean enabled, String prompt) { controller.updateGatheringLlm(enabled, prompt).ifPresent(sender::gatheringLlm); }
    public void saveApiConfig(String url, String key, String model) { controller.saveApiConfig(url, key, model).ifPresent(sender::apiConfig); }
    public void testApiConnection(String url, String key, String model) { sender.testApi(controller.testApiConnection(url, key, model)); }
    public void updateGatheringTarget(String value, boolean tag, CopperGolemGatheringTargetPayload.TargetSet set, CopperGolemGatheringTargetPayload.Action action) { controller.updateGatheringTarget(value, tag, set, action).ifPresent(sender::gatheringTarget); }

    interface CommandSender {
        void operation(CopperGolemMenuClientController.OperationCommand command);
        void mode(CopperGolemMenuClientController.ModeCommand command);
        void bindingLlm(CopperGolemMenuClientController.BindingLlmCommand command);
        void bindingCache(CopperGolemMenuClientController.BindingCacheCommand command);
        void gatheringLlm(CopperGolemMenuClientController.GatheringLlmCommand command);
        void apiConfig(CopperGolemMenuClientController.ApiConfigCommand command);
        void testApi(CopperGolemMenuClientController.TestApiCommand command);
        void gatheringTarget(CopperGolemMenuClientController.GatheringTargetCommand command);

        static CommandSender network() {
            return new CommandSender() {
                @Override public void operation(CopperGolemMenuClientController.OperationCommand c) { CopperGolemMenuActions.operation(c.golemId(), c.running(), c.revision()); }
                @Override public void mode(CopperGolemMenuClientController.ModeCommand c) { CopperGolemMenuActions.mode(c.golemId(), c.mode(), c.revision()); }
                @Override public void bindingLlm(CopperGolemMenuClientController.BindingLlmCommand c) { CopperGolemMenuActions.bindingLlm(c.golemId(), c.dimension(), c.x(), c.y(), c.z(), c.enabled(), c.prompt(), c.revision()); }
                @Override public void bindingCache(CopperGolemMenuClientController.BindingCacheCommand c) { CopperGolemMenuActions.bindingCache(c.golemId(), c.dimension(), c.x(), c.y(), c.z(), c.value(), c.tag(), c.allowed(), c.revision()); }
                @Override public void gatheringLlm(CopperGolemMenuClientController.GatheringLlmCommand c) { CopperGolemMenuActions.gatheringLlm(c.golemId(), c.enabled(), c.prompt(), c.revision()); }
                @Override public void apiConfig(CopperGolemMenuClientController.ApiConfigCommand c) { CopperGolemMenuActions.saveApi(c.golemId(), c.apiUrl(), c.apiKey(), c.model(), c.revision()); }
                @Override public void testApi(CopperGolemMenuClientController.TestApiCommand c) { CopperGolemMenuActions.testApi(c.apiUrl(), c.apiKey(), c.model()); }
                @Override public void gatheringTarget(CopperGolemMenuClientController.GatheringTargetCommand c) { CopperGolemMenuActions.gatheringTarget(c.golemId(), c.value(), c.tag(), c.set(), c.action(), c.revision()); }
            };
        }
    }
}
