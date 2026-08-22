package dev.totem.automata.containersafety;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.lang.reflect.Method;

/** Optional fail-closed bridge for a real player's protected-container action. */
public final class LocksmithPlayerAccessBridge {
    private static volatile Adapter adapter;
    private static volatile boolean resolved;

    private LocksmithPlayerAccessBridge() {
    }

    public static boolean mayExtract(ServerPlayer player, ServerLevel level, BlockPos position) {
        return invoke(true, player, level, position);
    }

    public static boolean mayInsert(ServerPlayer player, ServerLevel level, BlockPos position) {
        return invoke(false, player, level, position);
    }

    private static boolean invoke(
            boolean extraction,
            ServerPlayer player,
            ServerLevel level,
            BlockPos position
    ) {
        if (!FabricLoader.getInstance().isModLoaded("totem-locksmith")) return true;
        Adapter current = adapter();
        if (current == null) return false;
        Method method = extraction ? current.extract() : current.insert();
        return invokePlayer(method, player, level, position);
    }

    static boolean invokePlayer(
            Method method,
            ServerPlayer player,
            ServerLevel level,
            BlockPos position
    ) {
        if (method == null) return false;
        try {
            return (boolean) method.invoke(null, player, level, position);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return false;
        }
    }

    private static Adapter adapter() {
        if (resolved) return adapter;
        synchronized (LocksmithPlayerAccessBridge.class) {
            if (resolved) return adapter;
            try {
                Class<?> api = Class.forName(
                        "dev.totem.locksmith.api.v1.LocksmithPlayerAccessApi");
                adapter = new Adapter(
                        api.getMethod("mayExtract", ServerPlayer.class,
                                ServerLevel.class, BlockPos.class),
                        api.getMethod("mayInsert", ServerPlayer.class,
                                ServerLevel.class, BlockPos.class));
            } catch (ReflectiveOperationException ignored) {
                adapter = null;
            }
            resolved = true;
            return adapter;
        }
    }

    private record Adapter(Method extract, Method insert) {
    }
}
