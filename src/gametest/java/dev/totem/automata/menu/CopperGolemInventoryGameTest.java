package dev.totem.automata.menu;

import dev.totem.automata.copper.CopperGolemData;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.List;

/** Item conservation through Minecraft's actual click dispatcher and persisted authority. */
public final class CopperGolemInventoryGameTest {
    @GameTest(maxTicks = 20)
    public void reopenedMenuReadsCompactedStorageWithoutRestoringExtractedItems(GameTestHelper helper) {
        Fixture f = fixture(helper);
        f.authority.setGatheringStorage(f.golem, List.of(new ItemStack(Items.COAL, 8), new ItemStack(Items.IRON_INGOT, 8)));
        f.menu.clicked(2, 0, ContainerInput.QUICK_MOVE, f.player);
        CopperGolemMenu reopened = new CopperGolemMenu(CopperGolemMenuRegistration.TYPE, 2,
                f.player.getInventory(), f.player, f.golem, f.authority);
        check(helper, reopened.getSlot(2).getItem().is(Items.IRON_INGOT)
                && reopened.getSlot(2).getItem().getCount() == 8 && reopened.getSlot(3).getItem().isEmpty(),
                "Reopening must reconstruct compact persisted storage without extracted coal");
        reopened.clicked(2, 0, ContainerInput.PICKUP, f.player);
        check(helper, reopened.getCarried().is(Items.IRON_INGOT) && reopened.getCarried().getCount() == 8
                && f.menu.getSlot(3).getItem().isEmpty() && f.authority.gatheringStorage(f.golem).isEmpty()
                && countCoal(f.player) == 8, "Both menu views must observe the authoritative extraction");
        helper.succeed();
    }

    @GameTest(maxTicks = 20)
    public void toolPickupPlacementAndShiftPreserveDamage(GameTestHelper helper) {
        Fixture f = fixture(helper);
        ItemStack tool = new ItemStack(Items.IRON_PICKAXE);
        tool.setDamageValue(17);
        f.authority.setGatheringTool(f.golem, tool);
        f.menu.clicked(1, 0, ContainerInput.PICKUP, f.player);
        check(helper, ItemStack.matches(f.menu.getCarried(), tool) && f.authority.gatheringTool(f.golem).isEmpty(),
                "Tool pickup must clear persisted tool and preserve components");
        f.menu.clicked(1, 0, ContainerInput.PICKUP, f.player);
        check(helper, f.menu.getCarried().isEmpty() && ItemStack.matches(f.authority.gatheringTool(f.golem), tool),
                "Tool placement must persist the same damaged tool");
        f.menu.clicked(1, 0, ContainerInput.QUICK_MOVE, f.player);
        int tools = 0;
        for (int slot = 0; slot < 36; slot++) {
            if (ItemStack.matches(f.player.getInventory().getItem(slot), tool)) tools++;
        }
        check(helper, tools == 1 && f.authority.gatheringTool(f.golem).isEmpty(),
                "Tool shift must transfer exactly one damaged tool");
        helper.succeed();
    }

    @GameTest(maxTicks = 20)
    public void runningGatheringRejectsToolAndStorageExtraction(GameTestHelper helper) {
        Fixture f = fixture(helper);
        ItemStack tool = new ItemStack(Items.IRON_PICKAXE);
        f.authority.setGatheringTool(f.golem, tool);
        f.authority.setGatheringStorage(f.golem, List.of(new ItemStack(Items.COAL, 8)));
        var tag = CopperGolemData.readEntityTag(f.golem);
        tag.putBoolean(CopperGolemData.TAG_TRANSPORT_ENABLED, true);
        CopperGolemData.writeEntityTag(f.golem, tag);
        for (int slot : new int[]{1, 2}) {
            f.menu.clicked(slot, 0, ContainerInput.PICKUP, f.player);
            f.menu.clicked(slot, 0, ContainerInput.QUICK_MOVE, f.player);
        }
        check(helper, !f.authority.canEditGatheringSlots(f.golem), "Running gathering must disable edits");
        check(helper, f.menu.getCarried().isEmpty(), "Running gathering must reject pickup");
        check(helper, f.player.getInventory().isEmpty(), "Running gathering must reject shift extraction");
        check(helper, ItemStack.matches(f.authority.gatheringTool(f.golem), tool), "Running gathering must retain tool");
        check(helper, f.authority.gatheringStorage(f.golem).getFirst().getCount() == 8,
                "Running gathering must retain storage");
        helper.succeed();
    }

    @GameTest(maxTicks = 20)
    public void mutableStackChangesPersistWithoutRestoringAutonomousStorage(GameTestHelper helper) {
        Fixture f = fixture(helper);
        f.authority.setFuel(f.golem, new ItemStack(Items.COAL, 10));
        ItemStack live = f.menu.getSlot(0).getItem();
        check(helper, live == f.menu.getSlot(0).getItem(), "Unchanged authority must preserve live stack identity");
        live.shrink(2);
        f.authority.setGatheringStorage(f.golem, List.of(new ItemStack(Items.IRON_INGOT, 4)));
        f.menu.getSlot(0).setChanged();
        check(helper, f.authority.fuel(f.golem).getCount() == 8
                && f.authority.gatheringStorage(f.golem).getFirst().getCount() == 4,
                "setChanged must persist live mutations and preserve unrelated autonomous changes");
        helper.succeed();
    }

    @GameTest(maxTicks = 20)
    public void pickupAllConservesFuelAndStorageAndKeepsOtherSlots(GameTestHelper helper) {
        Fixture f = fixture(helper);
        f.authority.setFuel(f.golem, new ItemStack(Items.COAL, 3));
        f.authority.setGatheringStorage(f.golem, List.of(new ItemStack(Items.COAL, 8), new ItemStack(Items.IRON_INGOT, 8)));
        f.menu.setCarried(new ItemStack(Items.COAL, 1));
        f.menu.clicked(18, 0, ContainerInput.PICKUP_ALL, f.player);
        check(helper, f.menu.getCarried().getCount() == 12 && f.authority.fuel(f.golem).isEmpty(),
                "Pickup all must remove every collected coal exactly once");
        check(helper, f.menu.getSlot(2).getItem().isEmpty() && f.menu.getSlot(3).getItem().is(Items.IRON_INGOT)
                && f.authority.gatheringStorage(f.golem).size() == 1,
                "Pickup all must preserve unmatched stacks and stable live indices");
        helper.succeed();
    }

    @GameTest(maxTicks = 20)
    public void partialFuelShiftPersistsRemainder(GameTestHelper helper) {
        Fixture f = fixture(helper);
        f.authority.setFuel(f.golem, new ItemStack(Items.COAL, 64));
        fill(f.player);
        f.player.getInventory().setItem(0, new ItemStack(Items.COAL, 63));
        f.menu.clicked(0, 0, ContainerInput.QUICK_MOVE, f.player);
        check(helper, f.player.getInventory().getItem(0).getCount() == 64 && f.authority.fuel(f.golem).getCount() == 63,
                "Partial fuel extraction must conserve 127 coal");
        helper.succeed();
    }

    @GameTest(maxTicks = 20)
    public void partialStorageShiftPersistsRemainder(GameTestHelper helper) {
        Fixture f = fixture(helper);
        f.authority.setGatheringStorage(f.golem, List.of(new ItemStack(Items.COAL, 16)));
        fill(f.player);
        f.player.getInventory().setItem(0, new ItemStack(Items.COAL, 63));
        f.menu.clicked(2, 0, ContainerInput.QUICK_MOVE, f.player);
        check(helper, f.player.getInventory().getItem(0).getCount() == 64 && f.authority.gatheringStorage(f.golem).getFirst().getCount() == 15,
                "Partial storage extraction must conserve 79 coal");
        helper.succeed();
    }

    @GameTest(maxTicks = 20)
    public void shiftIntoExistingFuelPersistsMergedStack(GameTestHelper helper) {
        Fixture f = fixture(helper);
        f.authority.setFuel(f.golem, new ItemStack(Items.COAL, 63));
        f.player.getInventory().setItem(9, new ItemStack(Items.COAL, 2));
        f.menu.clicked(18, 0, ContainerInput.QUICK_MOVE, f.player);
        check(helper, f.authority.fuel(f.golem).getCount() == 64 && countCoal(f.player) == 1,
                "Shift merge must conserve 65 coal, including fallback inventory movement");
        helper.succeed();
    }

    @GameTest(maxTicks = 20)
    public void ordinaryPickupPlaceAndRightClickConserveFuel(GameTestHelper helper) {
        Fixture f = fixture(helper);
        f.authority.setFuel(f.golem, new ItemStack(Items.COAL, 10));
        f.menu.clicked(0, 1, ContainerInput.PICKUP, f.player);
        check(helper, f.menu.getCarried().getCount() == 5 && f.authority.fuel(f.golem).getCount() == 5, "Right pickup must split fuel");
        f.menu.clicked(0, 1, ContainerInput.PICKUP, f.player);
        check(helper, f.menu.getCarried().getCount() == 4 && f.authority.fuel(f.golem).getCount() == 6, "Right place must persist one fuel");
        f.menu.clicked(0, 0, ContainerInput.PICKUP, f.player);
        check(helper, f.menu.getCarried().isEmpty() && f.authority.fuel(f.golem).getCount() == 10, "Left merge must persist fuel");
        f.menu.clicked(0, 0, ContainerInput.PICKUP, f.player);
        check(helper, f.menu.getCarried().getCount() == 10 && f.authority.fuel(f.golem).isEmpty(), "Left pickup must clear persisted fuel");
        helper.succeed();
    }

    @GameTest(maxTicks = 20)
    public void storageRemovalKeepsLaterLiveSlotIndices(GameTestHelper helper) {
        Fixture f = fixture(helper);
        f.authority.setGatheringStorage(f.golem, List.of(new ItemStack(Items.COAL, 8), new ItemStack(Items.IRON_INGOT, 8)));
        f.menu.clicked(2, 0, ContainerInput.QUICK_MOVE, f.player);
        check(helper, f.menu.getSlot(2).getItem().isEmpty() && f.menu.getSlot(3).getItem().is(Items.IRON_INGOT),
                "Persisted compaction must not shift another item into the clicked live slot");
        f.menu.clicked(3, 0, ContainerInput.QUICK_MOVE, f.player);
        check(helper, f.authority.gatheringStorage(f.golem).isEmpty() && countCoal(f.player) == 8,
                "Full extraction must remove both storage stacks exactly once");
        f.authority.setFuel(f.golem, new ItemStack(Items.COAL, 12));
        f.menu.clicked(0, 0, ContainerInput.QUICK_MOVE, f.player);
        check(helper, f.authority.fuel(f.golem).isEmpty() && countCoal(f.player) == 20, "Full fuel extraction must clear persisted fuel");
        helper.succeed();
    }

    @GameTest(maxTicks = 20)
    public void autonomousChangesAreReadWithoutOverwritingOtherSlots(GameTestHelper helper) {
        Fixture f = fixture(helper);
        f.authority.setFuel(f.golem, new ItemStack(Items.COAL, 10));
        f.authority.setGatheringStorage(f.golem, List.of(new ItemStack(Items.IRON_INGOT, 8)));
        f.menu.broadcastChanges();
        f.authority.setFuel(f.golem, new ItemStack(Items.COAL, 9));
        f.authority.setGatheringStorage(f.golem, List.of(new ItemStack(Items.GOLD_INGOT, 6)));
        f.menu.clicked(0, 1, ContainerInput.PICKUP, f.player);
        check(helper, f.menu.getCarried().getCount() == 5 && f.authority.fuel(f.golem).getCount() == 4,
                "Click must observe fuel consumed since last menu broadcast");
        check(helper, f.authority.gatheringStorage(f.golem).getFirst().is(Items.GOLD_INGOT)
                && f.menu.getSlot(2).getItem().getCount() == 6, "Fuel edits must preserve autonomous storage updates");
        f.menu.setCarried(ItemStack.EMPTY);
        f.menu.clicked(2, 0, ContainerInput.PICKUP, f.player);
        check(helper, f.menu.getCarried().is(Items.GOLD_INGOT) && f.menu.getCarried().getCount() == 6
                && f.authority.gatheringStorage(f.golem).isEmpty(), "Storage click must use the latest autonomous contents");
        helper.succeed();
    }

    private static Fixture fixture(GameTestHelper helper) {
        CopperGolem golem = (CopperGolem) BuiltInRegistries.ENTITY_TYPE
                .getValue(Identifier.withDefaultNamespace("copper_golem"))
                .create(helper.getLevel(), EntitySpawnReason.COMMAND);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        // Login hooks may provide a manual; isolate inventory conservation fixtures.
        player.getInventory().clearContent();
        var tag = CopperGolemData.readEntityTag(golem);
        tag.putString(CopperGolemData.TAG_MODE, "gathering");
        tag.putBoolean(CopperGolemData.TAG_TRANSPORT_ENABLED, false);
        CopperGolemData.writeEntityTag(golem, tag);
        var authority = new PersistedCopperGolemMenuAuthority((viewer, target) -> {});
        return new Fixture(golem, player, authority,
                new CopperGolemMenu(CopperGolemMenuRegistration.TYPE, 1, player.getInventory(), player, golem, authority));
    }
    private static void fill(ServerPlayer player) {
        for (int slot = 0; slot < 36; slot++) player.getInventory().setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
    }
    private static int countCoal(ServerPlayer player) {
        int count = 0;
        for (int slot = 0; slot < 36; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(Items.COAL)) count += stack.getCount();
        }
        return count;
    }
    private static void check(GameTestHelper helper, boolean condition, String message) {
        if (!condition) throw helper.assertionException(message);
    }
    private record Fixture(CopperGolem golem, ServerPlayer player, PersistedCopperGolemMenuAuthority authority, CopperGolemMenu menu) {}
}
