package dev.totem.automata.network;

import dev.totem.automata.containersafety.RemnantBackpackBridge;
import dev.totem.automata.copper.CopperGolemActivityResolver;
import dev.totem.automata.copper.CopperGolemBinding;
import dev.totem.automata.copper.CopperGolemData;
import dev.totem.automata.copper.CopperGolemFuelService;
import dev.totem.automata.copper.GatheringConfiguration;
import dev.totem.automata.copper.GatheringLlmState;
import dev.totem.automata.copper.GolemLlmState;
import dev.totem.automata.copper.SortingBindingService;
import dev.totem.automata.copper.SortingDestinationService;
import dev.totem.automata.copper.SortingLlmState;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ai.behavior.TransportItemsBetweenContainers;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * Builds and sends the preserved Copper Wrench menu snapshot entirely from
 * Automata-owned persisted state.
 *
 * <p>The cutover composition passes this sender to both the menu opener and
 * payload handler. It is deliberately inert until that composition is
 * enabled, so the currently pinned DeadRecall authority remains singular.</p>
 */
public final class PersistedCopperGolemSnapshotSender implements BiConsumer<net.minecraft.server.level.ServerPlayer, CopperGolem> {
    private static final String GATHERING_TOOL = "deadrecall_gathering_tool_stack";
    private static final String GATHERING_STORAGE = "deadrecall_gathering_storage_stack";

    @Override
    public void accept(net.minecraft.server.level.ServerPlayer player, CopperGolem golem) {
        ServerPlayNetworking.send(player, snapshot(player, golem));
    }

    public CopperWrenchBindingsPayload snapshot(net.minecraft.server.level.ServerPlayer player, CopperGolem golem) {
        if (!(golem.level() instanceof ServerLevel level)) {
            throw new IllegalArgumentException("Copper Golem snapshots require a server level");
        }
        CompoundTag tag = CopperGolemData.readEntityTag(golem);
        CopperGolemData.migrate(tag);
        var activity = CopperGolemActivityResolver.resolveAndPersist(golem, level, tag);
        CopperGolemData.writeEntityTag(golem, tag);

        MinecraftServer server = level.getServer();
        GolemLlmState.Config llm = GolemLlmState.read(tag);
        GatheringLlmState.Config gatheringLlm = GatheringLlmState.read(tag);
        ItemStack fuel = CopperGolemFuelService.readFuelStack(tag);
        ItemStack tool = CopperGolemData.readItemStack(tag, GATHERING_TOOL);
        ItemStack storage = CopperGolemData.readItemStack(tag, GATHERING_STORAGE);
        List<CopperWrenchBindingsPayload.BindingEntry> bindings = SortingBindingService.getBindings(tag).stream()
                .map(binding -> bindingEntry(server, tag, binding))
                .toList();

        return new CopperWrenchBindingsPayload(
                golem.getUUID(),
                tag.getIntOr(CopperGolemData.TAG_REVISION, 0),
                tag.getBooleanOr(CopperGolemData.TAG_TRANSPORT_ENABLED, false),
                CopperGolemData.mode(tag).id(),
                activity.id(),
                itemId(fuel), fuel.getCount(), tag.getIntOr(CopperGolemData.TAG_FUEL_TICKS, 0),
                itemId(tool), tool.getCount(), tool.isDamageableItem() ? tool.getDamageValue() : 0,
                tool.isDamageableItem() ? tool.getMaxDamage() : 0,
                itemId(storage), storage.getCount(),
                llm.apiUrl(),
                AutomataPayloadPermissions.canManageServerConfiguration(player) ? llm.apiKey() : "",
                llm.model(),
                (int) SortingLlmState.read(tag).stream().filter(SortingLlmState.Config::enabled).count(),
                SortingBindingService.getSourceContainer(tag).map(binding -> sourceEntry(server, binding)).orElse(null),
                gatheringArea(tag),
                GatheringConfiguration.manualTargets(tag),
                gatheringLlm.enabled(), gatheringLlm.prompt(),
                gatheringLlm.allowedBlockIds().size() + gatheringLlm.deniedBlockIds().size(),
                gatheringLlm.allowedTags().size() + gatheringLlm.deniedTags().size(),
                gatheringLlm.allowedBlockIds(), gatheringLlm.deniedBlockIds(), gatheringLlm.allowedTags(), gatheringLlm.deniedTags(),
                bindings
        );
    }

    private static CopperWrenchBindingsPayload.GatheringAreaEntry gatheringArea(CompoundTag tag) {
        return GatheringConfiguration.readArea(tag).map(area -> {
            BlockPos a = area.cornerA().orElse(BlockPos.ZERO);
            BlockPos b = area.cornerB().orElse(BlockPos.ZERO);
            return new CopperWrenchBindingsPayload.GatheringAreaEntry(
                    area.dimension().identifier().toString(), area.cornerA().isPresent(), a.getX(), a.getY(), a.getZ(),
                    area.cornerB().isPresent(), b.getX(), b.getY(), b.getZ());
        }).orElse(null);
    }

    private static CopperWrenchBindingsPayload.BindingEntry sourceEntry(MinecraftServer server, CopperGolemBinding binding) {
        ServerLevel level = server.getLevel(binding.dimension());
        BlockPos pos = binding.containerPos();
        if (level == null || !level.isLoaded(pos)) {
            return unloadedEntry(binding, false, "", List.of(), List.of(), List.of(), List.of());
        }
        Item display = level.getBlockState(pos).getBlock().asItem();
        boolean available = level.getBlockState(pos).is(BlockTags.COPPER_CHESTS)
                && level.getBlockEntity(pos) instanceof Container;
        if (display == Items.AIR) {
            display = available ? Items.CHEST : Items.BARRIER;
        }
        return new CopperWrenchBindingsPayload.BindingEntry(
                binding.dimension().identifier().toString(), pos.getX(), pos.getY(), pos.getZ(),
                BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).toString(),
                BuiltInRegistries.ITEM.getKey(display).toString(), true, available, false, "", 0, 0,
                List.of(), List.of(), List.of(), List.of());
    }

    private static CopperWrenchBindingsPayload.BindingEntry bindingEntry(
            MinecraftServer server, CompoundTag tag, CopperGolemBinding binding) {
        SortingLlmState.Config llm = SortingLlmState.get(tag, binding);
        ServerLevel level = server.getLevel(binding.dimension());
        BlockPos pos = binding.containerPos();
        if (level == null || !level.isLoaded(pos)) {
            return unloadedEntry(binding, llm.enabled(), llm.prompt(), llm.allowedItemIds(), llm.deniedItemIds(), llm.allowedTags(), llm.deniedTags());
        }
        Item display = level.getBlockState(pos).getBlock().asItem();
        Container container = boundContainer(level, pos);
        boolean available = container != null;
        if (display == Items.AIR) {
            display = available ? Items.CHEST : Items.BARRIER;
        }
        List<String> preview = available
                ? acceptedPreview(container, llm)
                : llm.allowedItemIds();
        return new CopperWrenchBindingsPayload.BindingEntry(
                binding.dimension().identifier().toString(), pos.getX(), pos.getY(), pos.getZ(),
                BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).toString(),
                BuiltInRegistries.ITEM.getKey(display).toString(), true, available, llm.enabled(), llm.prompt(),
                llm.allowedItemIds().size() + llm.deniedItemIds().size(), llm.allowedTags().size() + llm.deniedTags().size(),
                preview, llm.deniedItemIds(), llm.allowedTags(), llm.deniedTags());
    }

    private static CopperWrenchBindingsPayload.BindingEntry unloadedEntry(
            CopperGolemBinding binding, boolean enabled, String prompt, List<String> allowedItems, List<String> deniedItems,
            List<String> allowedTags, List<String> deniedTags) {
        return new CopperWrenchBindingsPayload.BindingEntry(
                binding.dimension().identifier().toString(), binding.containerPos().getX(), binding.containerPos().getY(), binding.containerPos().getZ(),
                "unloaded", BuiltInRegistries.ITEM.getKey(Items.CHEST).toString(), false, false, enabled, prompt,
                allowedItems.size() + deniedItems.size(), allowedTags.size() + deniedTags.size(),
                allowedItems, deniedItems, allowedTags, deniedTags);
    }

    private static Container boundContainer(ServerLevel level, BlockPos pos) {
        if (level.getBlockState(pos).is(BlockTags.COPPER_CHESTS)) {
            return null;
        }
        TransportItemsBetweenContainers.TransportItemTarget target =
                TransportItemsBetweenContainers.TransportItemTarget.tryCreatePossibleTarget(pos, level);
        return target == null ? null : target.container();
    }

    private static List<String> acceptedPreview(Container container, SortingLlmState.Config llm) {
        Set<String> result = new LinkedHashSet<>();
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.isEmpty() || RemnantBackpackBridge.isBackpack(stack)) {
                continue;
            }
            String itemId = itemId(stack);
            if (llm.deniedItemIds().contains(itemId)
                    || llm.deniedTags().stream().anyMatch(stack.typeHolder().tags().map(tag -> tag.location().toString()).toList()::contains)) {
                continue;
            }
            if (SortingDestinationService.canAccept(container, stack.copyWithCount(1))) {
                result.add(itemId);
            }
        }
        for (String itemId : llm.allowedItemIds()) {
            if (!llm.deniedItemIds().contains(itemId)) {
                result.add(itemId);
            }
        }
        return List.copyOf(result);
    }

    private static String itemId(ItemStack stack) {
        return stack.isEmpty() ? BuiltInRegistries.ITEM.getKey(Items.AIR).toString()
                : BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }
}
