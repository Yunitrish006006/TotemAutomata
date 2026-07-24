package dev.totem.automata.copper;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Container;

import java.util.Optional;

/** Resolves the persisted copper-source binding into a live gathering home container. */
public final class GatheringHomeResolver {
    private GatheringHomeResolver() { }
    public static Optional<Home> resolve(net.minecraft.nbt.CompoundTag tag, ServerLevel level) {
        return SortingBindingService.getSourceContainer(tag).filter(binding -> binding.dimension().equals(level.dimension()))
                .filter(binding -> level.isLoaded(binding.containerPos()))
                .filter(binding -> level.getBlockState(binding.containerPos()).is(BlockTags.COPPER_CHESTS))
                .flatMap(binding -> level.getBlockEntity(binding.containerPos()) instanceof Container container ? Optional.of(new Home(binding, container)) : Optional.empty());
    }
    public record Home(CopperGolemBinding binding, Container container) { }
}
