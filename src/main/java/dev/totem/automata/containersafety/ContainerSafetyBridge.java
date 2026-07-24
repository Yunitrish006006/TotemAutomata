package dev.totem.automata.containersafety;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;

import java.lang.reflect.Method;

/** Optional adapter for portable-container policy without a required module dependency. */
public final class ContainerSafetyBridge {
    private static volatile ExternalPolicy externalPolicy;
    private static volatile boolean resolved;

    private ContainerSafetyBridge() {
    }

    public static boolean mayInsertIntoContainer(Container target, ItemStack incoming) {
        ExternalPolicy policy = externalPolicy();
        if (policy != null) {
            return policy.mayInsertIntoContainer(target, incoming);
        }
        return !(target instanceof ShulkerBoxBlockEntity) || incoming.getItem().canFitInsideContainerItems();
    }

    public static boolean mayInsertIntoPortableContainer(ItemStack incoming) {
        ExternalPolicy policy = externalPolicy();
        return policy != null ? policy.mayInsertIntoPortableContainer(incoming) : incoming.getItem().canFitInsideContainerItems();
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
        if (!FabricLoader.getInstance().isModLoaded("totem-container-safety")) {
            return null;
        }
        try {
            Class<?> api = Class.forName("dev.totem.containersafety.api.v1.PortableContainerSafetyApi");
            Object policy = api.getMethod("policy").invoke(null);
            Class<?> policyType = Class.forName("dev.totem.containersafety.api.v1.PortableContainerPolicy");
            Method mayInsertIntoContainer = policyType.getMethod(
                    "mayInsertIntoContainer", Container.class, ItemStack.class);
            Method mayInsertIntoPortableContainer = policyType.getMethod(
                    "mayInsertIntoPortableContainer", ItemStack.class);
            return new ExternalPolicy(policy, mayInsertIntoContainer, mayInsertIntoPortableContainer);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private record ExternalPolicy(Object delegate, Method containerMethod, Method portableMethod) {
        boolean mayInsertIntoContainer(Container target, ItemStack incoming) {
            return invoke(containerMethod, target, incoming);
        }

        boolean mayInsertIntoPortableContainer(ItemStack incoming) {
            return invoke(portableMethod, incoming);
        }

        private boolean invoke(Method method, Object... arguments) {
            try {
                return (boolean) method.invoke(delegate, arguments);
            } catch (ReflectiveOperationException exception) {
                return false;
            }
        }
    }
}
