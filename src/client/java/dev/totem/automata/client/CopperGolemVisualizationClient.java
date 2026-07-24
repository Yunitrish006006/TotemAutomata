package dev.totem.automata.client;

import dev.totem.automata.network.CopperGolemVisualizationPayload;
import dev.totem.automata.network.RequestCopperGolemVisualizationPayload;
import dev.totem.automata.copper.CopperWrenchSelection;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/** Client-only visualization of the authoritative Copper Golem snapshot. */
public final class CopperGolemVisualizationClient {
    private static final int REQUEST_INTERVAL_TICKS = 40, DRAW_INTERVAL_TICKS = 8, MAX_LINE_PARTICLES = 48;
    private static UUID heldGolemId;
    private static String heldDimension = "";
    private static int requestCooldown, drawCooldown;
    private static CopperGolemVisualizationPayload cachedPayload;

    private CopperGolemVisualizationClient() { }

    /** Called only by the future complete Automata client cutover. */
    public static void initialize() {
        ClientTickEvents.END_CLIENT_TICK.register(CopperGolemVisualizationClient::tick);
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
            cachedPayload = null; requestCooldown = 0; drawCooldown = 0;
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
        line(level, center, payload.source(), dimension, ParticleTypes.WAX_ON);
        if ("gathering".equals(payload.mode())) {
            area(level, payload.gatheringArea(), dimension); target(level, payload.gatheringTarget(), dimension);
        } else for (CopperGolemVisualizationPayload.PosEntry entry : payload.destinations()) {
            line(level, center, entry, dimension, entry.available() ? ParticleTypes.HAPPY_VILLAGER : ParticleTypes.SMOKE);
        }
    }

    private static void line(ClientLevel level, Vec3 start, CopperGolemVisualizationPayload.PosEntry entry, String dimension, ParticleOptions particle) {
        if (entry == null || !dimension.equals(entry.dimension())) return;
        drawLine(level, start, Vec3.atCenterOf(new BlockPos(entry.x(), entry.y(), entry.z())),
                entry.available() ? particle : ParticleTypes.SMOKE, .9D);
    }

    private static void area(ClientLevel level, CopperGolemVisualizationPayload.AreaEntry area, String dimension) {
        if (area == null || !dimension.equals(area.dimension())) return;
        if (area.hasCornerA()) corner(level, area.cornerAX(), area.cornerAY(), area.cornerAZ());
        if (area.hasCornerB()) corner(level, area.cornerBX(), area.cornerBY(), area.cornerBZ());
        if (!area.hasCornerA() || !area.hasCornerB()) return;
        int x1 = Math.min(area.cornerAX(), area.cornerBX()), y1 = Math.min(area.cornerAY(), area.cornerBY()), z1 = Math.min(area.cornerAZ(), area.cornerBZ());
        int x2 = Math.max(area.cornerAX(), area.cornerBX()) + 1, y2 = Math.max(area.cornerAY(), area.cornerBY()) + 1, z2 = Math.max(area.cornerAZ(), area.cornerBZ()) + 1;
        edge(level, x1,y1,z1,x2,y1,z1); edge(level, x1,y1,z2,x2,y1,z2); edge(level, x1,y2,z1,x2,y2,z1); edge(level, x1,y2,z2,x2,y2,z2);
        edge(level, x1,y1,z1,x1,y2,z1); edge(level, x2,y1,z1,x2,y2,z1); edge(level, x1,y1,z2,x1,y2,z2); edge(level, x2,y1,z2,x2,y2,z2);
        edge(level, x1,y1,z1,x1,y1,z2); edge(level, x2,y1,z1,x2,y1,z2); edge(level, x1,y2,z1,x1,y2,z2); edge(level, x2,y2,z1,x2,y2,z2);
    }

    private static void target(ClientLevel level, CopperGolemVisualizationPayload.PosEntry target, String dimension) {
        if (target == null || !dimension.equals(target.dimension())) return;
        Vec3 center = Vec3.atCenterOf(new BlockPos(target.x(), target.y(), target.z()));
        for (int i = 0; i < 5; i++) level.addParticle(ParticleTypes.END_ROD, center.x, center.y - .35D + i * .25D, center.z, 0, .01D, 0);
    }

    private static void corner(ClientLevel level, int x, int y, int z) {
        Vec3 center = Vec3.atCenterOf(new BlockPos(x, y, z));
        level.addParticle(ParticleTypes.END_ROD, center.x, center.y, center.z, 0, .01D, 0);
        level.addParticle(ParticleTypes.WAX_ON, center.x, center.y + .25D, center.z, 0, 0, 0);
    }
    private static void edge(ClientLevel level, double x1,double y1,double z1,double x2,double y2,double z2) { drawLine(level, new Vec3(x1,y1,z1), new Vec3(x2,y2,z2), ParticleTypes.ELECTRIC_SPARK, 1.2D); }
    private static void drawLine(ClientLevel level, Vec3 from, Vec3 to, ParticleOptions particle, double spacing) {
        int points = Math.max(2, Math.min(MAX_LINE_PARTICLES, (int) Math.ceil(from.distanceTo(to) / Math.max(.25D, spacing))));
        for (int i = 0; i <= points; i++) { double t = i / (double) points; level.addParticle(particle, from.x + (to.x-from.x)*t, from.y + (to.y-from.y)*t, from.z + (to.z-from.z)*t, 0,0,0); }
    }
    private static void clear() { heldGolemId = null; heldDimension = ""; requestCooldown = 0; drawCooldown = 0; cachedPayload = null; }
}
