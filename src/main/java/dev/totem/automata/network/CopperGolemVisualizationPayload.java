package dev.totem.automata.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Clientbound Copper Golem path and gathering-area visualization snapshot. */
public record CopperGolemVisualizationPayload(
        UUID golemId, boolean valid, String dimension, double golemX, double golemY, double golemZ,
        String mode, String activity, PosEntry source, AreaEntry gatheringArea, PosEntry gatheringTarget,
        List<PosEntry> destinations) implements CustomPacketPayload {
    public static final Type<CopperGolemVisualizationPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("deadrecall", "copper_golem_visualization"));
    private static final int MAX_POS_ENTRIES = 128;
    public record PosEntry(String dimension, int x, int y, int z, boolean available) { }
    public record AreaEntry(String dimension, boolean hasCornerA, int cornerAX, int cornerAY, int cornerAZ,
                            boolean hasCornerB, int cornerBX, int cornerBY, int cornerBZ) { }
    public static final StreamCodec<FriendlyByteBuf, CopperGolemVisualizationPayload> CODEC = StreamCodec.of(
            (buf, p) -> { buf.writeUUID(p.golemId()); buf.writeBoolean(p.valid()); buf.writeUtf(p.dimension(), 128);
                buf.writeDouble(p.golemX()); buf.writeDouble(p.golemY()); buf.writeDouble(p.golemZ());
                buf.writeUtf(p.mode(), 32); buf.writeUtf(p.activity(), 64); writeOptionalPos(buf, p.source());
                writeOptionalArea(buf, p.gatheringArea()); writeOptionalPos(buf, p.gatheringTarget()); writePosList(buf, p.destinations()); },
            buf -> new CopperGolemVisualizationPayload(buf.readUUID(), buf.readBoolean(), buf.readUtf(128),
                    buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readUtf(32), buf.readUtf(64),
                    readOptionalPos(buf), readOptionalArea(buf), readOptionalPos(buf), readPosList(buf)));
    private static void writeOptionalPos(FriendlyByteBuf b, PosEntry e) { b.writeBoolean(e != null); if (e != null) writePos(b, e); }
    private static PosEntry readOptionalPos(FriendlyByteBuf b) { return b.readBoolean() ? readPos(b) : null; }
    private static void writeOptionalArea(FriendlyByteBuf b, AreaEntry a) {
        b.writeBoolean(a != null); if (a == null) return; b.writeUtf(a.dimension(), 128); b.writeBoolean(a.hasCornerA());
        b.writeInt(a.cornerAX()); b.writeInt(a.cornerAY()); b.writeInt(a.cornerAZ()); b.writeBoolean(a.hasCornerB());
        b.writeInt(a.cornerBX()); b.writeInt(a.cornerBY()); b.writeInt(a.cornerBZ()); }
    private static AreaEntry readOptionalArea(FriendlyByteBuf b) { if (!b.readBoolean()) return null; return new AreaEntry(
            b.readUtf(128), b.readBoolean(), b.readInt(), b.readInt(), b.readInt(), b.readBoolean(), b.readInt(), b.readInt(), b.readInt()); }
    private static void writePosList(FriendlyByteBuf b, List<PosEntry> entries) {
        int size = Math.min(entries.size(), MAX_POS_ENTRIES); b.writeInt(size); for (int i = 0; i < size; i++) writePos(b, entries.get(i)); }
    private static List<PosEntry> readPosList(FriendlyByteBuf b) {
        int size = Math.min(b.readInt(), MAX_POS_ENTRIES); List<PosEntry> entries = new ArrayList<>(size);
        for (int i = 0; i < size; i++) entries.add(readPos(b)); return entries; }
    private static void writePos(FriendlyByteBuf b, PosEntry e) { b.writeUtf(e.dimension(), 128); b.writeInt(e.x()); b.writeInt(e.y()); b.writeInt(e.z()); b.writeBoolean(e.available()); }
    private static PosEntry readPos(FriendlyByteBuf b) { return new PosEntry(b.readUtf(128), b.readInt(), b.readInt(), b.readInt(), b.readBoolean()); }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
