package dev.totem.automata.containersafety;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;

import java.lang.reflect.Method;

/** Optional adapter for Remnant's portable-container policy without a required dependency. */
public final class ContainerSafetyBridge {
    private static volatile ExternalPolicy externalPolicy;
    private static volatile boolean resolved;

    private ContainerSafetyBridge() {
    }

    public static boolean mayInsertIntoContainer(Container target, ItemStack incoming) {
        ExternalPolicy policy = externalPolicy();
        if (policy != null) {
            boolean allowed = policy.mayInsertIntoContainer(target, incoming);
            if (!allowed) {
                policy.reportRejectedAutomation(target, incoming, "copper_golem_transfer");
            }
            return allowed;
        }
        return !(target instanceof ShulkerBoxBlockEntity) || incoming.getItem().canFitInsideContainerItems();
    }

    public static boolean mayInsertIntoPortableContainer(ItemStack incoming) {
        ExternalPolicy policy = externalPolicy();
        return policy != null ? policy.mayInsertIntoPortableContainer(incoming) : incoming.getItem().canFitInsideContainerItems();
    }

    public static boolean mayInsertIntoBackpack(ItemStack incoming) {
        ExternalPolicy policy = externalPolicy();
        return policy != null
                ? policy.mayInsertIntoBackpack(incoming)
                : incoming.getItem().canFitInsideContainerItems();
    }

    private static ExternalPolicy externalPolicy() {
        if (resolved) {
            return externalPolicy;
        }
        synchronized (ContainerSafetyBridge.class) {
            if (resolved) {
                return externalPolicy;
            }
            externalPolicy = loadExternalPolicy();
            resolved = true;
            return externalPolicy;
        }
    }

    private static ExternalPolicy loadExternalPolicy() {
        if (!FabricLoader.getInstance().isModLoaded("totem-remnant")) {
            return null;
        }
        try {
            Class<?> api = Class.forName("dev.totem.remnant.api.v1.PortableContainerSafetyApi");
            Method mayInsertIntoContainer = api.getMethod(
                    "mayInsertIntoContainer", Container.class, ItemStack.class);
            Method mayInsertIntoPortableContainer = api.getMethod(
                    "mayInsertIntoPortableContainer", ItemStack.class);
            Method mayInsertIntoBackpack = api.getMethod(
                    "mayInsertIntoBackpack", ItemStack.class);
            Method reportRejectedAutomation = api.getMethod(
                    "reportRejectedAutomation", Container.class, ItemStack.class, String.class);
            return new ExternalPolicy(
                    mayInsertIntoContainer,
                    mayInsertIntoPortableContainer,
                    mayInsertIntoBackpack,
                    reportRejectedAutomation
            );
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private record ExternalPolicy(
            Method containerMethod,
            Method portableMethod,
            Method backpackMethod,
            Method rejectionMethod
    ) {
        boolean mayInsertIntoContainer(Container target, ItemStack incoming) {
            return invoke(containerMethod, target, incoming);
        }

        boolean mayInsertIntoPortableContainer(ItemStack incoming) {
            return invoke(portableMethod, incoming);
        }

        boolean mayInsertIntoBackpack(ItemStack incoming) {
            return invoke(backpackMethod, incoming);
        }

        void reportRejectedAutomation(Container target, ItemStack incoming, String route) {
            try {
                rejectionMethod.invoke(null, target, incoming, route);
            } catch (ReflectiveOperationException ignored) {
                // Policy denial remains authoritative even if diagnostics are unavailable.
            }
        }

        private boolean invoke(Method method, Object... arguments) {
            try {
                return (boolean) method.invoke(null, arguments);
            } catch (ReflectiveOperationException exception) {
                return false;
            }
        }
    }
}
