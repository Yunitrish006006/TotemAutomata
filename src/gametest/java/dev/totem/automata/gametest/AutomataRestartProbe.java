package dev.totem.automata.gametest;

import dev.totem.automata.copper.CopperGolemActivity;
import dev.totem.automata.copper.CopperGolemBinding;
import dev.totem.automata.copper.CopperGolemData;
import dev.totem.automata.copper.CopperGolemFuelService;
import dev.totem.automata.copper.CopperGolemMode;
import dev.totem.automata.copper.GatheringToolPolicy;
import dev.totem.automata.copper.SortingBindingService;
import dev.totem.automata.excavation.TotemExcavationHammerAdapter;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

/** Verifies that Automata's preserved Copper Golem state survives a real server restart. */
public final class AutomataRestartProbe implements ModInitializer {
    private static final String PHASE_ENV = "TOTEM_AUTOMATA_RESTART_PROBE_PHASE";
    private static final String MARKER_DIRECTORY_ENV = "TOTEM_AUTOMATA_RESTART_PROBE_MARKER_DIR";
    private static final String UUID_FILE = "automata-copper-golem.uuid";
    private static final String PROBE_MARKER = "totem_automata_restart_probe";
    private static final BlockPos POS = new BlockPos(72, 200, 72);
    private static final BlockPos SOURCE_POS = POS.offset(3, 0, 0);
    private static final BlockPos DESTINATION_POS = POS.offset(-3, 0, 0);
    private static final int CHUNK_X = SectionPos.blockToSectionCoord(POS.getX());
    private static final int CHUNK_Z = SectionPos.blockToSectionCoord(POS.getZ());

    @Override
    public void onInitialize() {
        String phase = System.getenv(PHASE_ENV);
        if (phase == null || phase.isBlank()) {
            return;
        }
        Path markerDirectory = markerDirectory();
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            ServerLevel level = server.overworld();
            level.setChunkForced(CHUNK_X, CHUNK_Z, true);
            level.getChunk(POS);
            ServerTickEvents.END_SERVER_TICK.register(new Session(phase, markerDirectory)::tick);
        });
    }

    private static void runPhase(MinecraftServer server, String phase, Path markerDirectory) {
        switch (phase) {
            case "seed" -> seed(server.overworld(), markerDirectory);
            case "verify" -> verify(server.overworld(), markerDirectory);
            case "standalone" -> verifyStandaloneRuntime();
            default -> throw new IllegalArgumentException("Unknown Automata restart probe phase: " + phase);
        }
    }

    private static void verifyStandaloneRuntime() {
        require(!TotemExcavationHammerAdapter.isAvailable(),
                "Standalone probe unexpectedly found Totem Excavation on the runtime classpath");
        require(GatheringToolPolicy.accepts(new ItemStack(Items.IRON_PICKAXE)),
                "Standalone probe rejected an existing valid gathering tool");
        require(!GatheringToolPolicy.accepts(new ItemStack(Items.STICK)),
                "Standalone probe accepted an invalid gathering tool");
    }

    private static void seed(ServerLevel level, Path markerDirectory) {
        if (findProbeGolem(level) != null) {
            throw new IllegalStateException("Seed phase found a stale Automata copper golem");
        }
        require(level.setBlockAndUpdate(POS.below(), Blocks.STONE.defaultBlockState()), "Could not create probe floor");
        require(level.setBlockAndUpdate(SOURCE_POS, copperChest().defaultBlockState()),
                "Could not create probe source container");
        require(level.setBlockAndUpdate(DESTINATION_POS, Blocks.CHEST.defaultBlockState()),
                "Could not create probe destination container");
        CopperGolem golem = constructGolem(level);
        golem.setPersistenceRequired();
        CompoundTag tag = CopperGolemData.readEntityTag(golem);
        tag.putBoolean(PROBE_MARKER, true);
        tag.putInt(CopperGolemData.TAG_DATA_VERSION, CopperGolemData.DATA_VERSION);
        tag.putInt(CopperGolemData.TAG_REVISION, 41);
        tag.putString(CopperGolemData.TAG_MODE, CopperGolemMode.GATHERING.id());
        tag.putBoolean(CopperGolemData.TAG_TRANSPORT_ENABLED, false);
        tag.putString(CopperGolemData.TAG_ACTIVITY, CopperGolemActivity.SEARCHING.id());
        tag.putInt(CopperGolemData.TAG_FUEL_TICKS, 731);
        CopperGolemData.writeItemStack(tag, CopperGolemData.TAG_FUEL_STACK,
                new ItemStack(Items.NETHER_STAR, 2), level.registryAccess());
        CopperGolemData.writeItemStack(tag, "deadrecall_gathering_tool_stack", namedStack(Items.IRON_PICKAXE, 11, "Automata restart tool"), level.registryAccess());
        CopperGolemData.writeItemStack(tag, "deadrecall_gathering_storage_stack", namedStack(Items.COBBLESTONE, 9, "Automata restart storage"), level.registryAccess());
        CopperGolemData.writeBindings(tag, List.of(new CopperGolemBinding(level.dimension(), DESTINATION_POS)));
        SortingBindingService.writeSourceContainer(tag, new CopperGolemBinding(level.dimension(), SOURCE_POS));
        CopperGolemData.writeEntityTag(golem, tag);
        require(level.addFreshEntity(golem), "Could not add Automata copper golem");
        write(markerDirectory.resolve(UUID_FILE), golem.getUUID().toString() + "\n");
        verifyState(level, golem);
    }

    private static void verify(ServerLevel level, Path markerDirectory) {
        CopperGolem golem = requireGolem(level, readUuid(markerDirectory));
        verifyState(level, golem);
        level.setChunkForced(CHUNK_X, CHUNK_Z, false);
    }

    private static void verifyState(ServerLevel level, CopperGolem golem) {
        CompoundTag tag = CopperGolemData.readEntityTag(golem);
        require(tag.getBooleanOr(PROBE_MARKER, false), "Probe marker did not persist");
        require(tag.getIntOr(CopperGolemData.TAG_DATA_VERSION, 0) == CopperGolemData.DATA_VERSION, "Data version did not persist");
        require(tag.getIntOr(CopperGolemData.TAG_REVISION, 0) == 41, "Revision did not persist");
        require(CopperGolemData.mode(tag) == CopperGolemMode.GATHERING, "Mode did not persist");
        require(!tag.getBooleanOr(CopperGolemData.TAG_TRANSPORT_ENABLED, true), "Stopped transport state did not persist");
        require(CopperGolemData.activity(tag) == CopperGolemActivity.SEARCHING, "Activity did not persist");
        require(CopperGolemData.readBindings(tag).equals(List.of(new CopperGolemBinding(level.dimension(), DESTINATION_POS))), "Bindings did not persist");
        require(SortingBindingService.getSourceContainer(tag)
                        .filter(binding -> binding.dimension().equals(level.dimension()) && binding.containerPos().equals(SOURCE_POS))
                        .isPresent(), "Source binding did not persist");
        ItemStack tool = CopperGolemData.readItemStack(tag, "deadrecall_gathering_tool_stack", level.registryAccess());
        require(tool.is(Items.IRON_PICKAXE) && tool.getDamageValue() == 11
                        && Component.literal("Automata restart tool").equals(tool.get(DataComponents.CUSTOM_NAME)),
                "Tool components did not persist");
        ItemStack storage = CopperGolemData.readItemStack(tag, "deadrecall_gathering_storage_stack", level.registryAccess());
        require(storage.is(Items.COBBLESTONE) && storage.getCount() == 9
                        && Component.literal("Automata restart storage").equals(storage.get(DataComponents.CUSTOM_NAME)),
                "Storage components did not persist");
        ItemStack fuel = CopperGolemData.readItemStack(tag, CopperGolemData.TAG_FUEL_STACK, level.registryAccess());
        require(fuel.is(Items.NETHER_STAR) && fuel.getCount() == 2,
                "Infinite fuel did not persist");
        require(tag.getIntOr(CopperGolemData.TAG_FUEL_TICKS, 0) == 731,
                "Paused finite fuel state did not persist beneath the Nether Star");
        require(CopperGolemFuelService.hasFuelAvailable(tag, level),
                "Reloaded Nether Star was not recognized as infinite fuel");
    }

    private static ItemStack namedStack(net.minecraft.world.item.Item item, int count, String name) {
        ItemStack stack = new ItemStack(item, count);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        if (stack.isDamageableItem()) {
            stack.setDamageValue(11);
        }
        return stack;
    }

    private static CopperGolem requireGolem(ServerLevel level, UUID id) {
        Entity entity = level.getEntity(id);
        if (entity instanceof CopperGolem golem) {
            return golem;
        }
        throw new IllegalStateException("Probe copper golem did not reload: " + id);
    }

    private static CopperGolem findProbeGolem(ServerLevel level) {
        return level.getEntitiesOfClass(CopperGolem.class, new net.minecraft.world.phys.AABB(POS).inflate(16),
                        golem -> CopperGolemData.readEntityTag(golem).getBooleanOr(PROBE_MARKER, false))
                .stream().findFirst().orElse(null);
    }

    private static CopperGolem constructGolem(ServerLevel level) {
        Object type = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.fromNamespaceAndPath("minecraft", "copper_golem"));
        require(type != null, "Missing minecraft:copper_golem entity type");
        try {
            for (Constructor<?> constructor : CopperGolem.class.getDeclaredConstructors()) {
                Class<?>[] types = constructor.getParameterTypes();
                if (types.length == 2 && types[0].isInstance(type) && types[1].isInstance(level)) {
                    constructor.setAccessible(true);
                    CopperGolem golem = (CopperGolem) constructor.newInstance(type, level);
                    golem.snapTo(POS.getX() + .5D, POS.getY(), POS.getZ() + .5D, 0, 0);
                    return golem;
                }
            }
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not construct probe copper golem", exception);
        }
        throw new IllegalStateException("No compatible CopperGolem constructor was found");
    }

    private static Block copperChest() {
        Block block = BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("minecraft", "copper_chest"));
        require(block != null && block != Blocks.AIR, "Missing minecraft:copper_chest block");
        return block;
    }

    private static Path markerDirectory() {
        String configured = System.getenv(MARKER_DIRECTORY_ENV);
        return configured == null || configured.isBlank()
                ? Path.of("automata-restart-probe").toAbsolutePath().normalize()
                : Path.of(configured).toAbsolutePath().normalize();
    }

    private static UUID readUuid(Path markerDirectory) {
        try {
            return UUID.fromString(Files.readString(markerDirectory.resolve(UUID_FILE), StandardCharsets.UTF_8).trim());
        } catch (IOException | IllegalArgumentException exception) {
            throw new IllegalStateException("Could not read Automata probe UUID", exception);
        }
    }

    private static void write(Path file, String content) {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, content, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not write probe marker " + file, exception);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    private static final class Session {
        private final String phase;
        private final Path markerDirectory;
        private int ticks = 100;
        private boolean executed;

        private Session(String phase, Path markerDirectory) {
            this.phase = phase;
            this.markerDirectory = markerDirectory;
        }

        private void tick(MinecraftServer server) {
            if (--ticks > 0) {
                return;
            }
            try {
                if (!executed) {
                    runPhase(server, phase, markerDirectory);
                    executed = true;
                    ticks = 40;
                    return;
                }
                write(markerDirectory.resolve(phase + ".ok"), "success\n");
                server.halt(false);
            } catch (Throwable throwable) {
                write(markerDirectory.resolve(phase + ".failure"), throwable + "\n");
                server.halt(false);
                throw new IllegalStateException("Automata restart probe failed in phase " + phase, throwable);
            }
        }
    }
}
