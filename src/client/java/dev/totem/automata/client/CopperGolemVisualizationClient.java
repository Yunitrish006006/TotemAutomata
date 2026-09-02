package dev.totem.automata.client;

import dev.totem.core.api.v1.client.world.TotemWorldOutlines;
import dev.totem.core.api.v1.client.world.WorldOutlineOcclusion;
import dev.totem.core.api.v1.client.world.WorldOutlineStyle;
import dev.totem.automata.network.CopperGolemVisualizationPayload;
import dev.totem.automata.network.RequestCopperGolemVisualizationPayload;
import dev.totem.automata.copper.CopperWrenchSelection;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

/** Client-only visualization of the authoritative Copper Golem snapshot. */
public final class CopperGolemVisualizationClient {
    private static final int REQUEST_INTERVAL_TICKS = 40, DRAW_INTERVAL_TICKS = 8;
    private static final float CONTAINER_LINK_WIDTH = 2.0F;
    private static final WorldOutlineStyle GATHERING_AREA_STYLE = new WorldOutlineStyle(
            0xFF4FC3F7,
            1.5F,
            WorldOutlineOcclusion.DEPTH_TESTED
    );
    private static final WorldOutlineStyle SOURCE_CONTAINER_STYLE = new WorldOutlineStyle(
            0xFFFFB74D,
            CONTAINER_LINK_WIDTH,
            WorldOutlineOcclusion.DEPTH_TESTED
    );
    private static final WorldOutlineStyle AVAILABLE_DESTINATION_STYLE = new WorldOutlineStyle(
            0xFF66BB6A,
            CONTAINER_LINK_WIDTH,
            WorldOutlineOcclusion.DEPTH_TESTED
    );
    private static final WorldOutlineStyle UNAVAILABLE_CONTAINER_STYLE = new WorldOutlineStyle(
            0xFFEF5350,
            CONTAINER_LINK_WIDTH,
            WorldOutlineOcclusion.DEPTH_TESTED
    );
    private static UUID heldGolemId;
    private static String heldDimension = "";
    private static int requestCooldown, drawCooldown;
    private static CopperGolemVisualizationPayload cachedPayload;
    private static CopperGolem cachedClientGolem;
    private static UUID lastGolemLookupId;
    private static long lastGolemLookupGameTime = Long.MIN_VALUE;

    private CopperGolemVisualizationClient() { }

    /** Called by Automata's client cutover composition. */
    public static void initialize() {
        ClientTickEvents.END_CLIENT_TICK.register(CopperGolemVisualizationClient::tick);
        LevelRenderEvents.BEFORE_GIZMOS.register(context -> renderWorldOutlines());
        ClientPlayNetworking.registerGlobalReceiver(CopperGolemVisualizationPayload.TYPE,
                (payload, context) -> context.client().execute(() -> accept(payload)));
    }

    private static void tick(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.level == null) { clear(); return; }
        UUID selected = selectedGolem(minecraft.player.getMainHandItem());
        if (selected == null) selected = selectedGolem(minecraft.player.getOffhandItem());
        String dimension = minecraft.level.dimension().identifier().toString();
        if (selected == null) { clear(); return; }
        if (!selected.equals(heldGolemId) || !dimension.equals(heldDimension)) {
            cachedPayload = null; cachedClientGolem = null;
            lastGolemLookupId = null; lastGolemLookupGameTime = Long.MIN_VALUE;
            requestCooldown = 0; drawCooldown = 0;
        }
        heldGolemId = selected; heldDimension = dimension;
        if (requestCooldown-- <= 0) { requestCooldown = REQUEST_INTERVAL_TICKS; request(selected); }
        if (drawCooldown-- <= 0) { drawCooldown = DRAW_INTERVAL_TICKS; draw(minecraft.level, selected, dimension); }
    }

    private static UUID selectedGolem(ItemStack stack) {
        return CopperWrenchSelection.selectedGolem(stack);
    }

    private static void request(UUID golemId) {
        if (ClientPlayNetworking.canSend(RequestCopperGolemVisualizationPayload.TYPE))
            ClientPlayNetworking.send(new RequestCopperGolemVisualizationPayload(golemId));
    }

    private static void accept(CopperGolemVisualizationPayload payload) {
        cachedPayload = heldGolemId != null && heldGolemId.equals(payload.golemId()) && payload.valid() ? payload : null;
    }

    private static void draw(ClientLevel level, UUID selected, String dimension) {
        CopperGolemVisualizationPayload payload = cachedPayload;
        if (payload == null || !payload.valid() || !selected.equals(payload.golemId()) || !dimension.equals(payload.dimension())) return;
        Vec3 center = new Vec3(payload.golemX(), payload.golemY() + .9D, payload.golemZ());
        if (payload.activity() != null && payload.activity().startsWith("blocked_")) {
            level.addParticle(ParticleTypes.SMOKE, center.x, center.y + .6D, center.z, 0, .04D, 0);
        }
        if ("gathering".equals(payload.mode())) {
            target(level, payload.gatheringTarget(), dimension);
        }
    }

    private static void renderWorldOutlines() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) return;
        UUID selected = selectedGolem(minecraft.player.getMainHandItem());
        if (selected == null) selected = selectedGolem(minecraft.player.getOffhandItem());
        String dimension = minecraft.level.dimension().identifier().toString();
        CopperGolemVisualizationPayload payload = cachedPayload;
        if (selected == null || payload == null || !payload.valid()
                || !selected.equals(payload.golemId())
                || !dimension.equals(payload.dimension())) return;
        Vec3 center = currentGolemCenter(
                minecraft.level,
                selected,
                minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(!minecraft.isPaused()),
                new Vec3(payload.golemX(), payload.golemY() + .9D, payload.golemZ())
        );
        addContainerLinks(center, payload.source(), payload.destinations(), payload.mode(), dimension);
        if ("gathering".equals(payload.mode())) {
            addGatheringAreaOutline(payload.gatheringArea(), dimension);
        }
    }

    /** Resolves the selected client entity once, then follows its interpolated position every render frame. */
    static Vec3 currentGolemCenter(
            ClientLevel level,
            UUID selected,
            float partialTick,
            Vec3 fallback) {
        CopperGolem golem = findClientGolem(level, selected);
        return golem == null ? fallback : golem.getPosition(partialTick).add(0.0D, 0.9D, 0.0D);
    }

    private static CopperGolem findClientGolem(ClientLevel level, UUID selected) {
        if (cachedClientGolem != null
                && !cachedClientGolem.isRemoved()
                && cachedClientGolem.level() == level
                && selected.equals(cachedClientGolem.getUUID())) {
            return cachedClientGolem;
        }
        long gameTime = level.getGameTime();
        if (selected.equals(lastGolemLookupId) && gameTime == lastGolemLookupGameTime) {
            return null;
        }
        cachedClientGolem = null;
        lastGolemLookupId = selected;
        lastGolemLookupGameTime = gameTime;
        for (Entity entity : level.entitiesForRendering()) {
            if (entity instanceof CopperGolem golem && selected.equals(golem.getUUID())) {
                cachedClientGolem = golem;
                return golem;
            }
        }
        return null;
    }

    static void addGatheringAreaOutline(
            CopperGolemVisualizationPayload.AreaEntry area,
            String dimension) {
        if (area == null || !dimension.equals(area.dimension())) return;
        if (area.hasCornerA() && area.hasCornerB()) {
            AABB bounds = AABB.encapsulatingFullBlocks(
                    new BlockPos(area.cornerAX(), area.cornerAY(), area.cornerAZ()),
                    new BlockPos(area.cornerBX(), area.cornerBY(), area.cornerBZ())
            ).inflate(0.002D);
            TotemWorldOutlines.cuboid(bounds, GATHERING_AREA_STYLE);
            return;
        }
        if (area.hasCornerA()) {
            TotemWorldOutlines.block(
                    new BlockPos(area.cornerAX(), area.cornerAY(), area.cornerAZ()),
                    GATHERING_AREA_STYLE
            );
        }
        if (area.hasCornerB()) {
            TotemWorldOutlines.block(
                    new BlockPos(area.cornerBX(), area.cornerBY(), area.cornerBZ()),
                    GATHERING_AREA_STYLE
            );
        }
    }

    static void addContainerLinks(
            Vec3 golemCenter,
            CopperGolemVisualizationPayload.PosEntry source,
            List<CopperGolemVisualizationPayload.PosEntry> destinations,
            String mode,
            String dimension) {
        addContainerLink(
                golemCenter,
                source,
                dimension,
                source != null && source.available()
                        ? SOURCE_CONTAINER_STYLE
                        : UNAVAILABLE_CONTAINER_STYLE
        );
        if ("gathering".equals(mode) || destinations == null) return;
        for (CopperGolemVisualizationPayload.PosEntry destination : destinations) {
            addContainerLink(
                    golemCenter,
                    destination,
                    dimension,
                    destination != null && destination.available()
                            ? AVAILABLE_DESTINATION_STYLE
                            : UNAVAILABLE_CONTAINER_STYLE
            );
        }
    }

    private static void addContainerLink(
            Vec3 golemCenter,
            CopperGolemVisualizationPayload.PosEntry entry,
            String dimension,
            WorldOutlineStyle style) {
        if (entry == null || !dimension.equals(entry.dimension())) return;
        TotemWorldOutlines.line(
                golemCenter,
                Vec3.atCenterOf(new BlockPos(entry.x(), entry.y(), entry.z())),
                style
        );
    }

    private static void target(ClientLevel level, CopperGolemVisualizationPayload.PosEntry target, String dimension) {
        if (target == null || !dimension.equals(target.dimension())) return;
        Vec3 center = Vec3.atCenterOf(new BlockPos(target.x(), target.y(), target.z()));
        for (int i = 0; i < 5; i++) level.addParticle(ParticleTypes.END_ROD, center.x, center.y - .35D + i * .25D, center.z, 0, .01D, 0);
    }

    private static void clear() {
        heldGolemId = null;
        heldDimension = "";
        requestCooldown = 0;
        drawCooldown = 0;
        cachedPayload = null;
        cachedClientGolem = null;
        lastGolemLookupId = null;
        lastGolemLookupGameTime = Long.MIN_VALUE;
    }
}
