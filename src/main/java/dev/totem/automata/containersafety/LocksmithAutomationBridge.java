package dev.totem.automata.containersafety;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.Container;

import java.lang.reflect.Method;
import java.util.UUID;

/** Optional fail-closed bridge to TotemLocksmith's stable v1 automation API. */
public final class LocksmithAutomationBridge {
    private static volatile Adapter adapter;
    private static volatile boolean resolved;

    private LocksmithAutomationBridge() {
    }

    public static boolean mayExtract(Container source, UUID operatorId) {
        return invoke("extract", source, operatorId);
    }

    public static boolean mayInsert(Container destination, UUID operatorId) {
        return invoke("insert", destination, operatorId);
    }

    private static boolean invoke(String operation, Container container, UUID operatorId) {
        if (!FabricLoader.getInstance().isModLoaded("totem-locksmith")) return true;
        Adapter current = adapter();
        if (current == null) return false;
        try {
            Method method = "extract".equals(operation) ? current.extract() : current.insert();
            return (boolean) method.invoke(null, container, operatorId);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return false;
        }
    }

    private static Adapter adapter() {
        if (resolved) return adapter;
        synchronized (LocksmithAutomationBridge.class) {
            if (resolved) return adapter;
            try {
                Class<?> api = Class.forName("dev.totem.locksmith.api.v1.LocksmithAutomationApi");
                adapter = new Adapter(
                        api.getMethod("mayExtract", Container.class, UUID.class),
                        api.getMethod("mayInsert", Container.class, UUID.class));
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
