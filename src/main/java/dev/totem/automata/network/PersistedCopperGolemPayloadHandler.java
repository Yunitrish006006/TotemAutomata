package dev.totem.automata.network;

import dev.totem.automata.TotemAutomata;
import dev.totem.automata.copper.CopperGolemBinding;
import dev.totem.automata.copper.CopperGolemData;
import dev.totem.automata.copper.CopperGolemLlmClient;
import dev.totem.automata.copper.CopperGolemMode;
import dev.totem.automata.copper.CopperGolemStateMutation;
import dev.totem.automata.copper.CopperGolemWrenchAccess;
import dev.totem.automata.copper.GatheringRuntimeState;
import dev.totem.automata.copper.SortingBindingService;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;

/**
 * External, server-authoritative implementation of the preserved Copper
 * Golem payload protocol.
 *
 * <p>It deliberately has no DeadRecall feature import. The final cutover
 * composition supplies the menu snapshot refresher and registers this object
 * once through {@link CopperGolemPayloadRegistration}; the additive entrypoint
 * does neither while DeadRecall remains the live authority.</p>
 */
public final class PersistedCopperGolemPayloadHandler implements CopperGolemPayloadHandler {
    private static final ExecutorService CONNECTION_TESTS = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "TotemAutomata-LLM-Connection");
        thread.setDaemon(true);
        return thread;
    });
    private static final Set<UUID> PENDING_CONNECTION_TESTS = ConcurrentHashMap.newKeySet();

    private final BiConsumer<ServerPlayer, CopperGolem> refresher;

    public PersistedCopperGolemPayloadHandler(BiConsumer<ServerPlayer, CopperGolem> refresher) {
        this.refresher = refresher == null ? (player, golem) -> { } : refresher;
    }

    @Override
    public void setMode(ServerPlayer player, CopperGolemModePayload payload) {
        withGolem(player, payload.golemId(), payload.revision(), golem -> {
            CompoundTag tag = CopperGolemData.readEntityTag(golem);
            CopperGolemMode requested = CopperGolemMode.fromId(payload.mode());
            if (CopperGolemData.mode(tag) == requested || !CopperGolemStateMutation.canSwitchMode(tag, !golem.getMainHandItem().isEmpty())) {
                refresh(player, golem);
                return;
            }
            CopperGolemStateMutation.setMode(tag, requested);
            CopperGolemData.writeEntityTag(golem, tag);
            if (requested == CopperGolemMode.SORTING) {
                golem.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            }
            resetTransportMemories(golem);
            refresh(player, golem);
        });
    }

    @Override
    public void setOperation(ServerPlayer player, CopperGolemOperationPayload payload) {
        withGolem(player, payload.golemId(), payload.revision(), golem -> {
            CompoundTag tag = CopperGolemData.readEntityTag(golem);
            CopperGolemStateMutation.setTransportEnabled(tag, payload.running());
            CopperGolemData.writeEntityTag(golem, tag);
            if (!payload.running() && CopperGolemData.mode(tag) == CopperGolemMode.GATHERING) {
                golem.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            }
            resetTransportMemories(golem);
            refresh(player, golem);
        });
    }

    @Override
    public void updateGatheringTarget(ServerPlayer player, CopperGolemGatheringTargetPayload payload) {
        withGolem(player, payload.golemId(), payload.revision(), golem -> {
            CompoundTag tag = CopperGolemData.readEntityTag(golem);
            if (CopperGolemData.mode(tag) != CopperGolemMode.GATHERING || payload.action() != CopperGolemGatheringTargetPayload.Action.REMOVE
                    || !validGatheringTarget(payload)) {
                refresh(player, golem);
                return;
            }
            CopperGolemStateMutation.removeGatheringTarget(tag, payload.value(), payload.tag(), payload.targetSet());
            CopperGolemData.writeEntityTag(golem, tag);
            resetTransportMemories(golem);
            refresh(player, golem);
        });
    }

    @Override
    public void requestVisualization(ServerPlayer player, RequestCopperGolemVisualizationPayload payload) {
        CopperGolemWrenchAccess.SelectionResult resolved = CopperGolemWrenchAccess.resolveSelectedGolem(player, payload.golemId());
        if (!resolved.available() || !CopperGolemWrenchAccess.holdsBoundWrench(player, payload.golemId())) {
            ServerPlayNetworking.send(player, invalidVisualization(player, payload.golemId()));
            return;
        }
        ServerPlayNetworking.send(player, visualization(resolved.golem()));
    }

    @Override
    public void saveLlmConfig(ServerPlayer player, SaveCopperGolemLlmConfigPayload payload) {
        if (!AutomataPayloadPermissions.canManageServerConfiguration(player)) {
            player.sendSystemMessage(Component.translatable("message.deadrecall.copper_wrench.llm_permission_modify").withStyle(ChatFormatting.RED));
            return;
        }
        withGolem(player, payload.golemId(), payload.revision(), golem -> {
            CompoundTag tag = CopperGolemData.readEntityTag(golem);
            CopperGolemStateMutation.configureGolemLlm(tag, payload.apiUrl(), payload.apiKey(), payload.model());
            CopperGolemData.writeEntityTag(golem, tag);
            resetTransportMemories(golem);
            player.sendSystemMessage(Component.translatable("message.deadrecall.copper_wrench.llm_config_updated").withStyle(ChatFormatting.GREEN));
            refresh(player, golem);
        });
    }

    @Override
    public void testLlmConnection(ServerPlayer player, TestCopperGolemLlmConnectionPayload payload) {
        if (!AutomataPayloadPermissions.canManageServerConfiguration(player)) {
            player.sendSystemMessage(Component.translatable("message.deadrecall.copper_wrench.llm_permission_test").withStyle(ChatFormatting.RED));
            return;
        }
        String apiUrl = normalize(payload.apiUrl());
        String apiKey = normalize(payload.apiKey());
        String model = normalize(payload.model());
        if (apiUrl.isBlank() || model.isBlank()) {
            player.sendSystemMessage(Component.translatable("message.deadrecall.copper_wrench.llm_test_missing_config").withStyle(ChatFormatting.RED));
            return;
        }
        if (!PENDING_CONNECTION_TESTS.add(player.getUUID())) {
            player.sendSystemMessage(Component.translatable("message.deadrecall.copper_wrench.llm_test_pending").withStyle(ChatFormatting.YELLOW));
            return;
        }
        player.sendSystemMessage(Component.translatable("message.deadrecall.copper_wrench.llm_test_started").withStyle(ChatFormatting.YELLOW));
        var server = player.level().getServer();
        if (server == null) {
            PENDING_CONNECTION_TESTS.remove(player.getUUID());
            return;
        }
        CONNECTION_TESTS.submit(() -> runConnectionTest(server, player.getUUID(), apiUrl, apiKey, model));
    }

    @Override
    public void updateBindingLlm(ServerPlayer player, UpdateCopperGolemBindingLlmPayload payload) {
        withBinding(player, payload.golemId(), payload.revision(), payload.dimension(), payload.x(), payload.y(), payload.z(), (golem, binding) -> {
            CompoundTag tag = CopperGolemData.readEntityTag(golem);
            CopperGolemStateMutation.configureBindingLlm(tag, binding, payload.enabled(), payload.prompt());
            CopperGolemData.writeEntityTag(golem, tag);
            resetTransportMemories(golem);
            refresh(player, golem);
        });
    }

    @Override
    public void updateBindingCache(ServerPlayer player, UpdateCopperGolemBindingCachePayload payload) {
        if (normalize(payload.value()).isBlank()) {
            return;
        }
        withBinding(player, payload.golemId(), payload.revision(), payload.dimension(), payload.x(), payload.y(), payload.z(), (golem, binding) -> {
            CompoundTag tag = CopperGolemData.readEntityTag(golem);
            CopperGolemStateMutation.moveBindingLlmCache(tag, binding, payload.value(), payload.tag(), payload.allowed());
            CopperGolemData.writeEntityTag(golem, tag);
            resetTransportMemories(golem);
            refresh(player, golem);
        });
    }

    @Override
    public void updateGatheringLlm(ServerPlayer player, UpdateCopperGolemGatheringLlmPayload payload) {
        withGolem(player, payload.golemId(), payload.revision(), golem -> {
            CompoundTag tag = CopperGolemData.readEntityTag(golem);
            if (CopperGolemData.mode(tag) != CopperGolemMode.GATHERING) {
                refresh(player, golem);
                return;
            }
            CopperGolemStateMutation.configureGatheringLlm(tag, payload.enabled(), payload.prompt());
            CopperGolemData.writeEntityTag(golem, tag);
            resetTransportMemories(golem);
            refresh(player, golem);
        });
    }

    private void runConnectionTest(net.minecraft.server.MinecraftServer server, UUID playerId, String apiUrl, String apiKey, String model) {
        long startedAt = System.currentTimeMillis();
        try {
            CopperGolemLlmClient.askConnectionTest(apiUrl, apiKey, model);
            long elapsed = System.currentTimeMillis() - startedAt;
            server.execute(() -> sendConnectionResult(server.getPlayerList().getPlayer(playerId),
                    Component.translatable("message.deadrecall.copper_wrench.llm_test_success", elapsed).withStyle(ChatFormatting.GREEN)));
        } catch (Exception exception) {
            String message = safeErrorMessage(exception);
            TotemAutomata.LOGGER.warn("Copper Golem LLM connection test failed: {}", message);
            server.execute(() -> sendConnectionResult(server.getPlayerList().getPlayer(playerId),
                    Component.translatable("message.deadrecall.copper_wrench.llm_test_failed", message).withStyle(ChatFormatting.RED)));
        } finally {
            PENDING_CONNECTION_TESTS.remove(playerId);
        }
    }

    private static void sendConnectionResult(ServerPlayer player, Component message) {
        if (player != null) {
            player.sendSystemMessage(message);
        }
    }

    private void withGolem(ServerPlayer player, UUID golemId, int revision, java.util.function.Consumer<CopperGolem> action) {
        CopperGolemWrenchAccess.Result resolved = CopperGolemWrenchAccess.validate(player, golemId, revision);
        if (resolved.allowed()) {
            action.accept(resolved.golem());
        }
    }

    private void withBinding(
            ServerPlayer player,
            UUID golemId,
            int revision,
            String dimensionId,
            int x,
            int y,
            int z,
            BiConsumer<CopperGolem, CopperGolemBinding> action
    ) {
        Identifier id = Identifier.tryParse(dimensionId);
        if (id == null) {
            return;
        }
        withGolem(player, golemId, revision, golem -> {
            CopperGolemBinding binding = new CopperGolemBinding(ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, id), new BlockPos(x, y, z));
            if (SortingBindingService.getBindings(CopperGolemData.readEntityTag(golem)).contains(binding)) {
                action.accept(golem, binding);
            }
        });
    }

    private void refresh(ServerPlayer player, CopperGolem golem) {
        refresher.accept(player, golem);
    }

    private static boolean validGatheringTarget(CopperGolemGatheringTargetPayload payload) {
        if (normalize(payload.value()).isBlank()) {
            return false;
        }
        if (payload.targetSet() != CopperGolemGatheringTargetPayload.TargetSet.MANUAL) {
            return true;
        }
        Identifier block = Identifier.tryParse(payload.value());
        return !payload.tag() && block != null && BuiltInRegistries.BLOCK.getOptional(block).isPresent();
    }

    private static void resetTransportMemories(CopperGolem golem) {
        golem.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        golem.getBrain().eraseMemory(MemoryModuleType.VISITED_BLOCK_POSITIONS);
        golem.getBrain().eraseMemory(MemoryModuleType.UNREACHABLE_TRANSPORT_BLOCK_POSITIONS);
        golem.getBrain().eraseMemory(MemoryModuleType.TRANSPORT_ITEMS_COOLDOWN_TICKS);
    }

    private static CopperGolemVisualizationPayload invalidVisualization(ServerPlayer player, UUID golemId) {
        return new CopperGolemVisualizationPayload(golemId, false, player.level().dimension().identifier().toString(),
                0, 0, 0, "", "", null, null, null, List.of());
    }

    private static CopperGolemVisualizationPayload visualization(CopperGolem golem) {
        CompoundTag tag = CopperGolemData.readEntityTag(golem);
        CopperGolemData.migrate(tag);
        CopperGolemData.writeEntityTag(golem, tag);
        net.minecraft.server.MinecraftServer server = golem.level() instanceof ServerLevel level ? level.getServer() : null;
        List<CopperGolemVisualizationPayload.PosEntry> destinations = SortingBindingService.getBindings(tag).stream()
                .map(binding -> visualizationPosition(server, binding)).toList();
        CopperGolemVisualizationPayload.PosEntry source = SortingBindingService.getSourceContainer(tag)
                .map(binding -> visualizationPosition(server, binding)).orElse(null);
        CopperGolemVisualizationPayload.AreaEntry area = dev.totem.automata.copper.GatheringConfiguration.readArea(tag)
                .map(value -> new CopperGolemVisualizationPayload.AreaEntry(
                        value.dimension().identifier().toString(),
                        value.cornerA().isPresent(),
                        value.cornerA().map(BlockPos::getX).orElse(0), value.cornerA().map(BlockPos::getY).orElse(0), value.cornerA().map(BlockPos::getZ).orElse(0),
                        value.cornerB().isPresent(),
                        value.cornerB().map(BlockPos::getX).orElse(0), value.cornerB().map(BlockPos::getY).orElse(0), value.cornerB().map(BlockPos::getZ).orElse(0)
                )).orElse(null);
        CopperGolemVisualizationPayload.PosEntry target = CopperGolemData.mode(tag) == CopperGolemMode.GATHERING
                ? GatheringRuntimeState.target(tag)
                .map(pos -> new CopperGolemVisualizationPayload.PosEntry(golem.level().dimension().identifier().toString(), pos.getX(), pos.getY(), pos.getZ(), true))
                .orElse(null)
                : null;
        return new CopperGolemVisualizationPayload(golem.getUUID(), true, golem.level().dimension().identifier().toString(),
                golem.getX(), golem.getY(), golem.getZ(), CopperGolemData.mode(tag).id(), CopperGolemData.activity(tag).id(),
                source, area, target, destinations);
    }

    private static CopperGolemVisualizationPayload.PosEntry visualizationPosition(net.minecraft.server.MinecraftServer server, CopperGolemBinding binding) {
        ServerLevel level = server == null ? null : server.getLevel(binding.dimension());
        boolean available = level != null && level.isLoaded(binding.containerPos());
        return new CopperGolemVisualizationPayload.PosEntry(binding.dimension().identifier().toString(), binding.containerPos().getX(),
                binding.containerPos().getY(), binding.containerPos().getZ(), available);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private static String safeErrorMessage(Exception exception) {
        String message = exception.getMessage();
        String normalized = message == null || message.isBlank() ? exception.getClass().getSimpleName() : message.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 220 ? normalized : normalized.substring(0, 220) + "...";
    }
}
